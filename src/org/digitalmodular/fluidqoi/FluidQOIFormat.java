package org.digitalmodular.fluidqoi;

/**
 * @author Mark Jeronimus
 */
// Created 2022-05-25
public enum FluidQOIFormat {
	//@formatter:off
	/** 24BPP                                                                      */ RGB888   (3, 0),
	/** 32BPP                                                                      */ RGBA8888 (4, 0),
	/** 15 bits RGB (5 bits red, 5 bits green, 5 bits blue)                        */ RGB555   (5, 0),
	/** 16 bits RGB (5 bits red, 6 bits green, 5 bits blue)                        */ RGB565   (6, 0);
	//@formatter:on

	/** The lower 5 bits of the code determine the FluidQOI subtype */
	public static final int ENCODER_CODE_MASK = 0x1F;
	/** The upper 3 bits of the code determine image properties used to parse and reconstruct images */
	public static final int META_CODE_MASK    = 0xE0;

	private final int  encoder;
	private final int  subFormat;
	private final byte code;

	FluidQOIFormat(int encoder, int subFormat) {
		this.encoder = encoder;
		this.subFormat = subFormat;
		code = (byte)(subFormat << 5 | encoder);
	}

	public int getEncoder() {
		return encoder;
	}

	public int getSubFormat() {
		return subFormat;
	}

	public byte code() {
		return code;
	}

	public static FluidQOIFormat fromCode(int code) {
		for (FluidQOIFormat value : values()) {
			if (value.code == code) {
				return value;
			}
		}

		return null;
	}
}
