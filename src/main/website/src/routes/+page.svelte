<script lang="ts">
	import Speedtest from '$lib/client/Speedtest.svelte';
	import { STServer } from '$lib/client/speedtest_browser';

	const servers = [
		new STServer('https://yeen.host:5202', 'Homelab')
	];
</script>

<Speedtest {servers} />