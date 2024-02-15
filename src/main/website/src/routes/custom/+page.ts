import { redirect } from '@sveltejs/kit';
import type { PageLoad } from './$types';

export const load = (async ({ url }) => {
	const address = url.searchParams.get('address');

	if (address) {
		return { address };
	} else {
		throw redirect(302, '/');
	}
}) satisfies PageLoad;
