package org.digitalmodular.fluidqoi.core;

/**
 * Superclass containing common elements for both encoder and decoder
 *
 * @author Mark Jeronimus
 */
// Created 2022-05-27
public class FluidQOICodec {
	protected int indexLength  = 0;
	protected int repeatLength = 0;

	protected int opIndex    = 0;
	protected int opRepeat   = 0;
	protected int opLuma222  = 0;
	protected int opLuma322  = 0;
	protected int opLuma433  = 0;
	protected int opLuma4444 = 0;
	protected int opLuma644  = 0;
	protected int opRGB555   = 0;
	protected int opRGB565   = 0;
	protected int opMask3    = 0;
	protected int opMask4    = 0;

	// Encoder/Decoder state
	protected int recentColorsIndex = 0;

	protected void setIndexLength(int indexLength, int opRepeat) {
		this.indexLength = indexLength;

		int remainingCodeSpace = 256 - opRepeat;
		if (indexLength < 1 || indexLength >= remainingCodeSpace) {
			throw new IllegalArgumentException("'indexLength' must be between 1 and " +
			                                   remainingCodeSpace + ": " + indexLength);
		}

		opIndex = 256 - indexLength;
		this.opRepeat = opRepeat;
		repeatLength = opIndex - opRepeat; // Alternatively: remainingCodeSpace - indexLength
	}

	protected void resetCodecState() {
		recentColorsIndex = 0;
	}
}
