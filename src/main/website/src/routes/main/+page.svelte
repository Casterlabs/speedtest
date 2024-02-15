<script lang="ts">
	import Speedtest from '$lib/client/Speedtest.svelte';
	import { STServer } from '$lib/client/speedtest_browser';

	const servers = [
		new STServer('http://localhost', 'Test'),
		new STServer('http://localhost2', 'Test2')
	];
</script>

<Speedtest {servers} />
