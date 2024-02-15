<script lang="ts">
	import Speedtest from '$lib/client/Speedtest.svelte';
	import { STServer } from '$lib/client/speedtest_browser';

	const servers = [
		new STServer('https://oci-tvwzgo.casterlabs.co:9052', 'Oracle Cloud (Virginia) #tvwzgo'),
		new STServer('https://oci-kzyqih.casterlabs.co:9052', 'Oracle Cloud (London) #kzyqih')
	];
</script>

<Speedtest {servers} />
