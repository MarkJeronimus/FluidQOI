package org.digitalmodular.fluidqoi;

/**
 * @author Mark Jeronimus
 */
// Created 2022-05-25
public class FluidQOIConfig {
	private int            indexLength;
	private FluidQOIFormat formatOverride = null;

	public FluidQOIConfig(int indexLength) {
		this.indexLength = indexLength;
	}

	public int getIndexLength() {
		return indexLength;
	}

	public FluidQOIConfig setIndexLength(int indexLength) {
		this.indexLength = indexLength;
		return this;
	}

	public FluidQOIFormat getFormatOverride() {
		return formatOverride;
	}

	public FluidQOIConfig setFormatOverride(FluidQOIFormat formatOverride) {
		this.formatOverride = formatOverride;
		return this;
	}
}
