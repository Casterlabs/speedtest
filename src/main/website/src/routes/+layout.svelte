<script>
	import CSSIntermediate from '$lib/client/CSSIntermediate.svelte';

	// import { onMount } from 'svelte';
	// import { browser, dev } from '$app/environment';
	// import { page } from '$app/stores';
	// import * as Swetrix from 'swetrix';

	// onMount(() => {
	// 	Swetrix.init('nfB52mJWenOg', { debug: dev, disabled: dev });
	// 	Swetrix.trackViews();
	// });

	// // Trigger a pageview everytime the user navigates.
	// // We use the `browser` env variable to ensure that we don't try to
	// // call trackViews while the component is doing SSR.
	// $: $page.url.pathname, browser && Swetrix.trackViews();
</script>

<CSSIntermediate>
	<main class="h-full">
		<slot />
	</main>
</CSSIntermediate>
