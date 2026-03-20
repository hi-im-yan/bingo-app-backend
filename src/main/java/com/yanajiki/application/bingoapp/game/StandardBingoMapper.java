package com.yanajiki.application.bingoapp.game;

import org.springframework.stereotype.Component;

/**
 * Standard 75-ball BINGO implementation of {@link NumberLabelMapper}.
 * <p>
 * Maps drawn numbers to their BINGO column letter according to the traditional layout:
 * </p>
 * <ul>
 *   <li><b>B</b>: 1–15</li>
 *   <li><b>I</b>: 16–30</li>
 *   <li><b>N</b>: 31–45</li>
 *   <li><b>G</b>: 46–60</li>
 *   <li><b>O</b>: 61–75</li>
 * </ul>
 * <p>
 * Examples: {@code toLabel(1)} → {@code "B-1"}, {@code toLabel(42)} → {@code "N-42"},
 * {@code toLabel(75)} → {@code "O-75"}.
 * </p>
 * <p>
 * This is registered as the default Spring bean for {@link NumberLabelMapper}.
 * To support a different game variant, implement {@link NumberLabelMapper} and
 * qualify the beans appropriately.
 * </p>
 */
@Component
public class StandardBingoMapper implements NumberLabelMapper {

	private static final int MIN_NUMBER = 1;
	private static final int MAX_NUMBER = 75;

	/** {@inheritDoc} */
	@Override
	public String toLabel(int number) {
		String letter;
		if (number <= 15) {
			letter = "B";
		} else if (number <= 30) {
			letter = "I";
		} else if (number <= 45) {
			letter = "N";
		} else if (number <= 60) {
			letter = "G";
		} else {
			letter = "O";
		}
		return letter + "-" + number;
	}

	/** {@inheritDoc} */
	@Override
	public int getMinNumber() {
		return MIN_NUMBER;
	}

	/** {@inheritDoc} */
	@Override
	public int getMaxNumber() {
		return MAX_NUMBER;
	}
}
