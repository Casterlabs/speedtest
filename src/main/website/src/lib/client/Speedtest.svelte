<script lang="ts">
	import { Scatter } from 'svelte-chartjs';
	import 'chart.js/auto';

	import { onMount } from 'svelte';
	import { STServer, type STServerTestResult } from '$lib/client/speedtest_browser';

	let STYLE = (q: string) => '';
	if (typeof window != 'undefined') {
		STYLE = (q: string) =>
			getComputedStyle(document.querySelector('#css-intermediate') as HTMLElement).getPropertyValue(
				q
			);
	}

	export let servers: STServer[];
	let currentServer: STServer | null = null;

	let showingDebug = false;
	let testRunning = false;
	let downloadTestResult: STServerTestResult;
	let uploadTestResult: STServerTestResult;

	let chart: any;
	let chartData = {
		datasets: [
			{
				label: 'Download',
				data: [],
				fill: false,
				tension: 0.3,
				showLine: true
			},
			{
				label: 'Upload',
				data: [],
				fill: false,
				tension: 0.3,
				showLine: true
			}
		]
	};

	async function test() {
		if (!currentServer) return;
		try {
			testRunning = true;
			reset();

			await currentServer.testDownload((i) => {
				downloadTestResult = i;
				if (chartData.datasets[0].data.length == 0) {
					i.progress = 0; // Annoying fix for log scale.
				}
				// @ts-ignore
				chartData.datasets[0].data.push({
					x: i.progress,
					y: i.speed_bps
				});
				chart.update();
			});

			await currentServer.testUpload((i) => {
				uploadTestResult = i;
				if (chartData.datasets[1].data.length == 0) {
					i.progress = 0; // Annoying fix for log scale.
				}
				// @ts-ignore
				chartData.datasets[1].data.push({
					x: i.progress,
					y: i.speed_bps
				});
				chart.update();
			});
		} finally {
			testRunning = false;
		}
	}

	function reset() {
		downloadTestResult = { elapsed_ms: 0, speed_bps: 0, speed_str: '...', progress: 0 };
		uploadTestResult = { elapsed_ms: 0, speed_bps: 0, speed_str: '...', progress: 0 };
		chartData.datasets[0].data = [];
		chartData.datasets[1].data = [];
	}
	reset();

	// @ts-ignore
	onMount(async () => {
		let mounted = true;

		await Promise.allSettled(
			servers.map(async (server) => {
				try {
					await server.probe();
				} catch (ignored) {}
			})
		);
		servers = servers; // Re-render

		async function pingLoop() {
			if (!mounted) return;

			if (!testRunning) {
				await Promise.allSettled(
					servers.map(async (server) => {
						try {
							if (server.alive) {
								await server.testPing();
							}
						} catch (e) {
							server.alive = false;
						}
						return;
					})
				);
				servers = servers; // Re-render

				if (currentServer == null) {
					currentServer =
						servers
							.filter((s) => s.alive) //
							.filter((s) => s.ping.average >= 0) //
							.sort((s1, s2) => s1.ping.average - s2.ping.average)[0] || null;
				} else {
					currentServer = currentServer;
				}
			}

			setTimeout(pingLoop, 1500);
		}

		pingLoop();

		return () => {
			mounted = false;
		};
	});

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
</script>

<div class="pt-6 mx-auto max-w-4xl">
	<div class="w-full h-96">
		<Scatter
			bind:chart
			data={chartData}
			options={{
				responsive: true,
				maintainAspectRatio: false,
				// @ts-ignore
				animations: false,
				elements: {
					point: {
						radius: 0
					}
				},
				plugins: {
					legend: {
						labels: {
							color: STYLE('--base11')
						}
					}
				},
				scales: {
					x: {
						type: 'linear',
						min: 0,
						border: {
							color: STYLE('--base11')
						},
						grid: {
							color: STYLE('--base4')
						},
						ticks: {
							callback(val, index) {
								return '';
							}
						}
					},
					y: {
						type: 'logarithmic',
						min: 0,
						border: {
							color: STYLE('--base11')
						},
						grid: {
							color: STYLE('--base4')
						},
						ticks: {
							color: STYLE('--base11'),
							callback(val, index) {
								// @ts-ignore
								return formatSpeed(val);
							}
						}
					}
				}
			}}
		/>
	</div>

	<div class="flex flex-row space-x-12 items-center">
		<table class="basis-3/4">
			<tr>
				<td class="text-right">Download Speed:</td>
				<td class="pl-2">{downloadTestResult?.speed_str}</td>
			</tr>
			<tr>
				<td class="text-right">Upload Speed:</td>
				<td class="pl-2">{uploadTestResult?.speed_str}</td>
			</tr>
			<tr>
				{#if servers.length == 1}
					<td class="text-right">Ping:</td>
					<td class="pl-2">
						{currentServer?.ping.average.toFixed(0)}ms
					</td>
				{:else}
					<td class="text-right">Server:</td>
					<td class="pl-2">
						{#if currentServer}
							<select
								class="bg-base-2 border border-base-4 rounded-md"
								bind:value={currentServer}
								disabled={testRunning}
							>
								<option value={null}> Automatic </option>
								{#each servers as server}
									<option value={server}>
										{server.name}
									</option>
								{/each}
							</select>
							@ {currentServer.ping.average.toFixed(0)}ms
						{:else}
							...
						{/if}
					</td>
				{/if}
			</tr>
		</table>

		<button
			on:click={test}
			disabled={testRunning}
			class:opacity-60={testRunning}
			class="bg-base-6 p-2 rounded"
		>
			Start Test
		</button>
	</div>

	<button class="block mt-12 text-sm" on:click={() => (showingDebug = !showingDebug)}>
		{#if showingDebug}
			Hide Debug Info <icon
				class="inline-block w-4 h-4 translate-y-0.5"
				data-icon="icon/chevron-up"
			/>
		{:else}
			Show Debug Info <icon
				class="inline-block w-4 h-4 translate-y-0.5"
				data-icon="icon/chevron-down"
			/>
		{/if}
	</button>
	{#if showingDebug}
		<div class="flex flex-col md:flex-row">
			<div class="basis-1/2 overflow-hidden">
				<h2 class="mt-2 text-lg font-bold">Download Test Result:</h2>
				<pre>{JSON.stringify(downloadTestResult, null, 2)}</pre>
				<h2 class="mt-2 text-lg font-bold">Upload Test Result:</h2>
				<pre>{JSON.stringify(uploadTestResult, null, 2)}</pre>
			</div>
			<div class="basis-1/2 overflow-hidden">
				<h2 class="mt-2 text-lg font-bold">Best Server:</h2>
				<pre>{JSON.stringify({ ...currentServer, ping: undefined }, null, 2)}</pre>
				<h2 class="mt-2 text-lg font-bold">Network Ping Samples:</h2>
				<pre>{JSON.stringify(currentServer?.ping, null, 0)}</pre>
			</div>
		</div>
	{/if}
</div>
