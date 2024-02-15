const iconCache: { [key: string]: string } = {};

function replaceIcon(element: HTMLElement) {
	if (element.hasAttribute('data-replaced')) return;

	const icon = element.getAttribute('data-icon') as string;
	element.setAttribute('data-replaced', 'true');

	if (iconCache[icon]) {
		element.innerHTML = iconCache[icon];
		console.debug('[App]', 'Loaded icon from cache:', icon);
	} else {
		const [iconType, iconPath] = icon.split('/');

		(async () => {
			try {
				let svg;

				switch (iconType) {
					case 'icon':
						const promises = [
							fetch(`/images/icons/solid/${iconPath}.svg`)
								.then((res) => {
									if (res.ok) {
										return res.text();
									} else {
										throw 'Status: ' + res.status;
									}
								})
								.then((s) => s.replace('<svg', '<svg class="icon-solid"')),

							fetch(`/images/icons/outline/${iconPath}.svg`)
								.then((res) => {
									if (res.ok) {
										return res.text();
									} else {
										throw 'Status: ' + res.status;
									}
								})
								.then((s) => s.replace('<svg', '<svg class="icon-outline"'))
						];

						svg = (await Promise.all(promises)).join('');
						break;

					case 'service':
						svg = await fetch(`/images/services/${iconPath}/icon.svg`)
							.then((res) => {
								if (res.ok) {
									return res.text();
								} else {
									throw 'Status: ' + res.status;
								}
							})
							.then((s) => s.replace('<svg', '<svg class="icon-service"'));
						break;
				}

				if (svg) {
					element.innerHTML = iconCache[icon] = svg;
					console.debug('[App]', 'Loaded icon:', icon);
				} else {
					throw 'Unknown icon: ' + icon;
				}
			} catch (e) {
				element.innerHTML = iconCache[icon] =
					'<div class="bg-red-500 h-full w-full text-white" title="MISSING ICON">X</div>'; // Visual error.
				console.error('[App]', 'Could not load icon', icon, 'due to an error:');
				console.error(e);
			}

			element.setAttribute('data-icon-type', iconType);
		})();
	}
}

export default function hook() {
	new MutationObserver((records) => {
		for (const element of document.querySelectorAll('icon')) {
			if (element.nodeName.toLowerCase() == 'icon') {
				replaceIcon(element as HTMLElement);
			}
		}
	}).observe(document.body, {
		subtree: true,
		attributes: true,
		childList: true
	});

	document.querySelectorAll('icon').forEach((e) => replaceIcon(e as HTMLElement));
}
