<script lang="ts">
	import Modal from '$lib/client/Modal.svelte';

	let showingCustomServerModal = false;
	let address = '';
</script>

{#if showingCustomServerModal}
	<Modal on:close={() => (showingCustomServerModal = false)}>
		<h1 slot="title">Enter the custom server URL</h1>

		<div class="w-96 flex flex-row">
			<input
				class="flex-1 bg-base-2 border border-base-4 rounded-l-md"
				placeholder="https://"
				bind:value={address}
			/>
			<a
				class="bg-base-2 border border-base-4 rounded-r-md px-4 py-2"
				href="/custom?address={address}"
			>
				Go
			</a>
		</div>
	</Modal>
{/if}

<h1 class="text-center text-xl pb-4 pt-10">Select which servers to use:</h1>
<div class="w-fit mx-auto flex flex-row space-x-4">
	<a
		href="/main"
		class="w-40 h-40 bg-base-4 border border-base-6 rounded-md flex flex-col items-center justify-center"
	>
		<icon class="inline-block align-middle" data-icon="icon/bolt" />
		<h2 class="text-sm mt-2">Main Servers</h2>
	</a>
	<button
		on:click={() => (showingCustomServerModal = true)}
		class="w-40 h-40 bg-base-4 border border-base-6 rounded-md flex flex-col items-center justify-center"
	>
		<icon class="inline-block align-middle" data-icon="icon/wrench" />
		<h2 class="text-sm mt-2">Custom Server</h2>
	</button>
</div>
