<script lang="ts">
	import Speedtest from '$lib/client/Speedtest.svelte';
	import { STServer } from '$lib/client/speedtest_browser';

	export let data;
	const servers = [new STServer(data.address, '')];
</script>

<Speedtest {servers} />
