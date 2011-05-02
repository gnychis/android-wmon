/*
 * Linux IEEE 802.15.4 userspace tools
 *
 * Copyright (C) 2008, 2009 Sismens AG
 *
 * Written-by: Dmitry Eremin-Solenikov
 * Written-by: Sergey Lapin
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; version 2 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <libcommon.h>
#include <stdlib.h>

struct shash_elem {
	const void *key;
	void *data;
	struct shash_elem *next;
	struct shash_elem **prev;
};

struct simple_hash {
	shash_hash hashfn;
	shash_eq eqfn;
	/* FIXME: it's a fake hash currently */
	struct shash_elem *elem;
};

struct simple_hash *shash_new(shash_hash hashfn, shash_eq eqfn)
{
	struct simple_hash *hash = calloc(1, sizeof(struct simple_hash));
	if (hash) {
		hash->hashfn = hashfn;
		hash->eqfn = eqfn;
	}

	return hash;
}
void *shash_insert(struct simple_hash *hash, const void *key, void *data)
{
	struct shash_elem *elem;
	void *old;

	for (elem = hash->elem; elem; elem = elem->next) {
		if (!hash->eqfn(elem->key, key)) {
			old = elem->data;
			elem->data = data;
			return old;
		}
	}

	elem = calloc(1, sizeof(*elem));
	elem->key = key;
	elem->data = data;

	elem->next = hash->elem;
	elem->prev = &hash->elem;
	hash->elem = elem;
	if (elem->next)
		elem->next->prev = &elem->next;

	return NULL;
}
void *shash_get(struct simple_hash *hash, const void *key)
{
	struct shash_elem *elem;

	for (elem = hash->elem; elem; elem = elem->next) {
		if (!hash->eqfn(elem->key, key)) {
			return elem->data;
		}
	}

	return NULL;
}
void *shash_drop(struct simple_hash *hash, const void *key)
{
	struct shash_elem *elem;

	for (elem = hash->elem; elem; elem = elem->next) {
		if (!hash->eqfn(elem->key, key)) {
			*(elem->prev) = elem->next;
		}
	}

	return elem;
}
