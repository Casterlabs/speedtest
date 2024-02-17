export declare type STServerTestResult = {
	elapsed_ms: number;
	speed_bps: number;
	speed_str: string;
	progress: number;
};

export declare type STServerPingData = {
	average: number;
	samples: number[];
};

export class STServer {
	address: string;
	name: string;
	alive: boolean = false;
	ping: STServerPingData = {
		average: -1,
		samples: []
	};
	serviceData: any = null;

	constructor(address: string, name: string) {
		this.address = address;
		this.name = name;
	}

	async probe() {
		try {
			if (!this.alive) {
				console.info(`SpeedTest / Probe @ ${this.name}`, 'Probing...');
				const json = await (await fetch(`${this.address}/test/service-data`)).json();
				if (!json.data) throw json;

				console.info(`SpeedTest / Probe @ ${this.name}`, 'Server is alive!');
				this.alive = true;
				this.serviceData = json.data;
			}
		} catch (e) {
			console.info(`SpeedTest / Probe @ ${this.name}`, 'Error whilst probing server:', e);
		}
	}

	testPing(): Promise<void> {
		return new Promise((resolve, reject) => {
			try {
				// XMLHttpRequest is a LOT faster and more precise than fetch.
				const xhr = new XMLHttpRequest();
				let startedAt: number;

				xhr.open('GET', `${this.address}/test/ping`, true);
				xhr.onreadystatechange = () => {
					if (xhr.readyState == 2) {
						const ping = Date.now() - startedAt;
						xhr.abort();

						this.ping.samples.push(ping);
						if (this.ping.samples.length > 10) {
							this.ping.samples.shift();
						}

						this.ping.average =
							this.ping.samples.reduce((a, b) => a + b, 0) / this.ping.samples.length;
						resolve();
					}
				};
				startedAt = Date.now();
				xhr.onerror = reject;
				xhr.send(null);
			} catch (e) {
				reject(e);
			}
		});
	}

	async testDownload(callback: (result: STServerTestResult) => void) {
		try {
			console.info(`SpeedTest / Probe @ ${this.name}`, 'Starting...', this);

			const xhr = new XMLHttpRequest();
			let start: number;

			xhr.open('PATCH', `${this.address}/test/download`, true);
			xhr.onreadystatechange = () => {
				if (xhr.readyState == XMLHttpRequest.HEADERS_RECEIVED) {
					start = Date.now();
					setTimeout(() => xhr.abort(), this.serviceData.time_limit);
				}
			};
			xhr.onprogress = (e) => progress(xhr, e, start, callback);
			xhr.send(null);

			await new Promise((resolve) => (xhr.onloadend = resolve));
		} catch (e) {
			console.error(`SpeedTest / Probe @ ${this.name}`, e);
		} finally {
			console.info(`SpeedTest / Probe @ ${this.name}`, 'Done!');
		}
	}

	async testUpload(callback: (result: STServerTestResult) => void) {
		try {
			console.info(`SpeedTest / Probe @ ${this.name}`, 'Starting...', this);

			const CHUNK_SIZE = 100 * 1000 * 100; // 100mb, seems to be a practical limit.

			const start = Date.now();
			const buffer = new ArrayBuffer(CHUNK_SIZE);
			let it = 0;

			const send = () =>
				new Promise<void>((resolve, reject) => {
					const xhr = new XMLHttpRequest();

					xhr.open('PATCH', `${this.address}/test/upload`, true);
					xhr.upload.onprogress = (e) => progress(xhr, e, start, callback, it * CHUNK_SIZE);
					xhr.upload.onerror = console.error;

					xhr.send(buffer);

					xhr.onloadend = () => resolve();
					xhr.onabort = () => reject();
				});

			while (true) {
				try {
					await send();
				} catch (e) {
					break;
				}
				it++;
			}
		} catch (e) {
			console.error(`SpeedTest / Probe @ ${this.name}`, e);
		} finally {
			console.info(`SpeedTest / Probe @ ${this.name}`, 'Done!');
		}
	}
}

function formatSpeed(speed_bps: number) {
	if (speed_bps <= 1 && speed_bps != 0) return '';

	let speed_str: string;
	if (speed_bps >= 1000000000000) {
		speed_str = (speed_bps / 1000000000000).toFixed(1) + 'tbps';
	} else if (speed_bps >= 1000000000) {
		speed_str = (speed_bps / 1000000000).toFixed(1) + 'gbps';
	} else if (speed_bps >= 1000000) {
		speed_str = (speed_bps / 1000000).toFixed(1) + 'mbps';
	} else if (speed_bps >= 1000) {
		speed_str = (speed_bps / 1000).toFixed(0) + 'kbps';
	} else {
		speed_str = speed_bps.toFixed(0) + 'bps';
	}

	speed_str = speed_str.replace(/\.0+/, ''); // Remove trailing 0s.
	return speed_str;
}

function progress(
	xhr: XMLHttpRequest,
	e: ProgressEvent,
	start: number,
	callback: (result: STServerTestResult) => void,
	offset = 0
) {
	const MAX_TEST_TIME = 10 * 1000;

	let elapsed_ms = Date.now() - start;
	let speed_bps = ((e.loaded + offset) * 8) / (elapsed_ms / 1000);
	let progress = elapsed_ms / MAX_TEST_TIME;

	if (progress > 1) {
		progress = 1; // CLAMP!
		xhr.abort();
	}

	if (Number.isNaN(speed_bps) || speed_bps == Infinity) {
		speed_bps = 0;
	}

	let speed_str = formatSpeed(speed_bps);

	callback({ elapsed_ms, speed_bps, speed_str, progress });
}
