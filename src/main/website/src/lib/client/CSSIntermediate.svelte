<script lang="ts">
	// Theme colors.
	import './css/colors/base/slate.css';
	import './css/colors/primary/blue.css';
	import './css/colors/overlay-black.css';
	import './css/colors/overlay-white.css';

	// The actual gravy.
	import './css/app.css';
	import './css/colors/base.css';
	import './css/colors/primary.css';
	import './css/colors/misc.css';

	import iconsHook from '$lib/client/icons';
	import { onMount } from 'svelte';
	import { isDarkTheme } from './app';

	const baseColor = 'mauve';
	const primaryColor = 'crimson';

	onMount(iconsHook);
</script>

<!--
	The sites's theming is handled with data-theme-base, data-theme-primary, and class:dark-theme (we include data-theme-dark for debugging).
	All of the css files to make this happen are imported above.
-->

<svelte:head>
	<meta name="theme-color" content={$isDarkTheme ? '#161618' : '#FDFCFD'} />
</svelte:head>

<div
	id="css-intermediate"
	class="relative w-full h-full bg-base-1 text-base-12 overflow-auto"
	class:dark-theme={$isDarkTheme}
	data-theme-dark={$isDarkTheme}
	data-theme-base={baseColor}
	data-theme-primary={primaryColor}
>
	<slot />

	<button
		class="absolute top-3 right-3 lg:right-4 text-base-11"
		on:click={() => {
			isDarkTheme.set(!$isDarkTheme);
			console.log('Switching theme to:', $isDarkTheme ? 'dark' : 'light');
			localStorage.setItem(
				'casterlabs:color_preference',
				JSON.stringify($isDarkTheme ? 'dark' : 'light')
			);
		}}
	>
		{#if $isDarkTheme}
			<icon class="w-5 h-5" data-icon="icon/moon" />
		{:else}
			<icon class="w-5 h-5" data-icon="icon/sun" />
		{/if}
	</button>
</div>

<style>
	#css-intermediate {
		--link: rgb(54, 100, 252);
		--error: rgb(224, 30, 30);
		--success: rgb(69, 204, 69);
	}

	#css-intermediate.dark-theme {
		--link: rgb(58, 137, 255);
		--error: rgb(252, 31, 31);
		--success: rgb(64, 187, 64);
	}
</style>
