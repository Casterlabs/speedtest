<span class="loader" />

<style>
	.loader {
		width: 20px;
		height: 20px;
		border-radius: 50%;
		display: inline-block;
		border-top: 3px solid var(--base12);
		border-right: 3px solid transparent;
		box-sizing: border-box;
		animation: rotation 1s linear infinite;
	}

	@keyframes rotation {
		0% {
			transform: rotate(0deg);
		}

		100% {
			transform: rotate(360deg);
		}
	}
</style>
