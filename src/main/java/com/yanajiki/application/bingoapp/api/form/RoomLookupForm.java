package com.yanajiki.application.bingoapp.api.form;

import java.util.List;

/**
 * Request body for looking up bingo rooms by a batch of creator hashes.
 * <p>
 * A {@code null} or empty {@code creatorHashes} list is legal and yields an empty result.
 * Unknown hashes are silently skipped — the endpoint never returns 404.
 * </p>
 *
 * @param creatorHashes the list of creator hashes to resolve into rooms
 */
public record RoomLookupForm(List<String> creatorHashes) {
}
