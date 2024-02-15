import { writable, type Writable } from 'svelte/store';

export const isDarkTheme = writable(true);

if (typeof window != 'undefined') {
	const userPreference = JSON.parse(localStorage.getItem('casterlabs:color_preference') as string);
	const browserPreference = window.matchMedia
		? window.matchMedia('(prefers-color-scheme: dark)')
			? 'dark'
			: 'light'
		: 'dark';

	console.debug('User theme preference:', userPreference);
	console.debug('Browser/system theme preference:', browserPreference);

	if (userPreference) {
		console.log('User has a theme preference, going with that.');
		isDarkTheme.set(userPreference == 'dark');
	} else {
		console.log('User has no theme preference yet, using browser/system preference.');
		isDarkTheme.set(browserPreference == 'dark');
	}

	isDarkTheme.subscribe((dark) => {
		console.log('Switching theme to:', dark ? 'dark' : 'light');
		localStorage.setItem('casterlabs:color_preference', JSON.stringify(dark ? 'dark' : 'light'));
	});
}
