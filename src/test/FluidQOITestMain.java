package test;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;

import org.digitalmodular.fluidqoi.FluidQOIConfig;
import org.digitalmodular.fluidqoi.FluidQOIFormat;
import org.digitalmodular.fluidqoi.FluidQOIImageDecoder;
import org.digitalmodular.fluidqoi.FluidQOIImageEncoder;

/**
 * @author Mark Jeronimus
 */
// Created 2022-05-22
@SuppressWarnings({"UseOfSystemOutOrSystemErr", "CallToPrintStackTrace"})
public final class FluidQOITestMain {
	@SuppressWarnings("StaticCollection")
	static final List<Path> files = new ArrayList<>(20000);

	private FluidQOITestMain() {
	}

	public static void main(String... args) throws IOException {
		FluidQOIImageEncoder.debugging = false;
		FluidQOIImageDecoder.debugging = false;

//		collectImageFilesRecursively(files, Paths.get("qoi_test_images"));
		collectImageFilesRecursively(files, Paths.get("images-pixelart-tiles"));
//		collectImageFilesRecursively(files, Paths.get("images-lance"));
//		collectImageFilesRecursively(files, Paths.get("qoi_benchmark_suite"));
		files.sort(Comparator.comparing(Path::getFileName));

		for (Path file : files) {
			BufferedImage[] images = convertImage(file);

			int mismatchPixel = compareImages(images);
			if (mismatchPixel >= 0) {
				System.out.println(
						file.getFileName() + ": Images differ at pixel " + mismatchPixel + " / " +
						images[0].getWidth() * images[0].getHeight() + " (" +
						mismatchPixel % images[2].getWidth() + ", " + mismatchPixel / images[2].getWidth() + ')');
			}
		}
	}

	static void collectImageFilesRecursively(Collection<Path> files, Path path) throws IOException {
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				super.visitFile(file, attrs);

				if (file.getFileName().toString().endsWith(".png"))
					files.add(file);

				return FileVisitResult.CONTINUE;
			}
		});
	}

	static BufferedImage[] convertImage(Path file) {
		BufferedImage image;
		try {
			image = ImageIO.read(file.toFile());
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
			//noinspection ReturnOfNull
			return null;
		}

		try {
			FluidQOIConfig config  = new FluidQOIConfig(16);
			ByteBuffer     qoiData = new FluidQOIImageEncoder(config).encode(image);
 			BufferedImage  image2  = new FluidQOIImageDecoder().decode(qoiData);

			BufferedImage image1 = new BufferedImage(image2.getWidth(), image2.getHeight(), image2.getType());
			Graphics2D    g      = image1.createGraphics();
			try {
				g.drawImage(image, 0, 0, null);
			} finally {
				g.dispose();
			}

//			String filename = file.getFileName().toString();
//			filename = filename.substring(0, filename.length() - 4) + ".qoi565";
//			Files.write(file.getParent().resolve(filename), qoi565.array());

			return new BufferedImage[]{image, image1, image2};
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
			return new BufferedImage[0];
		}
	}

	static int compareImages(BufferedImage[] images) {
		DataBuffer dataBuffer1 = images[1].getRaster().getDataBuffer();
		DataBuffer dataBuffer2 = images[2].getRaster().getDataBuffer();

		if (dataBuffer2 instanceof DataBufferByte) {
			byte[] pixels1 = ((DataBufferByte)dataBuffer1).getData();
			byte[] pixels2 = ((DataBufferByte)dataBuffer2).getData();
			for (int i = 0; i < pixels1.length; i++) {
				if (pixels1[i] != pixels2[i]) {
					return i / images[2].getColorModel().getNumComponents();
				}
			}
		} else if (dataBuffer2 instanceof DataBufferUShort) {
			short[] pixels1 = ((DataBufferUShort)dataBuffer1).getData();
			short[] pixels2 = ((DataBufferUShort)dataBuffer2).getData();
			for (int i = 0; i < pixels1.length; i++) {
				if (pixels1[i] != pixels2[i]) {
					return i;
				}
			}
		} else if (dataBuffer2 instanceof DataBufferInt) {
			int[] pixels1 = ((DataBufferInt)dataBuffer1).getData();
			int[] pixels2 = ((DataBufferInt)dataBuffer2).getData();
			for (int i = 0; i < pixels1.length; i++) {
				if (pixels1[i] != pixels2[i]) {
					return i;
				}
			}
		} else {
			throw new AssertionError("Unknown data buffer: " + dataBuffer2.getClass());
		}

		return -1;
	}
}
