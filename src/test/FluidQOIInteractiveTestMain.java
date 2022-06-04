package test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.digitalmodular.fluidqoi.FluidQOIImageDecoder;
import org.digitalmodular.fluidqoi.FluidQOIImageEncoder;

/**
 * @author Mark Jeronimus
 */
// Created 2022-05-22
public class FluidQOIInteractiveTestMain extends JPanel {
	private static int fileIndex = 0;

	private BufferedImage image1 = null;
	private BufferedImage image2 = null;

	public static void main(String... args) throws IOException {
//		FluidQOITestMain.collectImageFilesRecursively(FluidQOITestMain.files, Paths.get("qoi_test_images"));
//		FluidQOITestMain.collectImageFilesRecursively(FluidQOITestMain.files, Paths.get("qoi_benchmark_suite"));
//		FluidQOITestMain.collectImageFilesRecursively(FluidQOITestMain.files, Paths.get("images-lance"));
		FluidQOITestMain.collectImageFilesRecursively(FluidQOITestMain.files, Paths.get("images-pixelart-tiles"));
		FluidQOITestMain.files.sort(Comparator.comparing(Path::getFileName));

		FluidQOIImageEncoder.debugging = true;
		FluidQOIImageDecoder.debugging = true;

		if (FluidQOITestMain.files.isEmpty()) {
			System.err.println("No files found");
			System.exit(1);
		}

//		fileIndex = ThreadLocalRandom.current().nextInt(files.size());
//		fileIndex = IntStream.range(0, FluidQOITestMain.files.size())
//		                     .filter(i -> {
//			                     Path file = FluidQOITestMain.files.get(i);
//			                     return file.getFileName().toString().startsWith("79E5B7F3");
//		                     })
//		                     .findFirst()
//		                     .orElse(0);

		SwingUtilities.invokeLater(() -> {
			JFrame f = new JFrame();
			f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

			f.setContentPane(new FluidQOIInteractiveTestMain());

			f.pack();
			f.setLocationRelativeTo(null);
			f.setVisible(true);
		});
	}

	@SuppressWarnings("OverridableMethodCallDuringObjectConstruction")
	public FluidQOIInteractiveTestMain() {
		super(null);
		setBackground(Color.WHITE);

		fileIndex %= FluidQOITestMain.files.size();

		setPreferredSize(new Dimension(512, 256));

		// Yes I know it's bad practice to do work on the EDT, but it's a cheap test class anyway.

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					fileIndex = (fileIndex + 1) % FluidQOITestMain.files.size();
				} else if (e.getButton() == MouseEvent.BUTTON3) {
					fileIndex =
							(fileIndex + FluidQOITestMain.files.size() - 1) % FluidQOITestMain.files.size();
				}

				// Yes I know it's bad practice to do work on the EDT, but it's a cheap test class anyway.
				convertImage();

				repaint();
			}
		});

		SwingUtilities.invokeLater(this::convertImage);
	}

	private void convertImage() {
		Path file = FluidQOITestMain.files.get(fileIndex);

		Frame topLevelAncestor = (Frame)getTopLevelAncestor();
		if (topLevelAncestor != null) {
			topLevelAncestor.setTitle(fileIndex + ": " + file.getFileName());
		}

		BufferedImage[] images = FluidQOITestMain.convertImage(file);
		image1 = images[1];
		image2 = images[2];

		int mismatchPixel = FluidQOITestMain.compareImages(images);
		if (mismatchPixel >= 0) {
			SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
					this,
					"Images differ at pixel " + mismatchPixel + " (" +
					mismatchPixel % images[2].getWidth() + ", " + mismatchPixel / images[2].getWidth() + ')',
					getClass().getSimpleName(),
					JOptionPane.ERROR_MESSAGE));
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (image1 == null)
			return;

		g.drawImage(image1,
		            0, 0, getWidth() / 2, getHeight(),
		            0, 0, image1.getWidth(), image1.getHeight(),
		            null);
		g.drawImage(image2,
		            getWidth() / 2, 0, getWidth(), getHeight(),
		            0, 0, image2.getWidth(), image2.getHeight(),
		            null);

		int scaleX = getWidth() / 2 / image1.getWidth();
		int scaleY = getHeight() / image1.getHeight();
		if (scaleX >= 16 && scaleY >= 16) {
			g.setColor(Color.DARK_GRAY);
			for (int u = 1; u < image1.getWidth() * 2; u++) {
				int x = u * getWidth() / (image1.getWidth() * 2);
				g.drawLine(x, 0, x, getHeight());
			}
			for (int v = 1; v < image1.getHeight(); v++) {
				int y = v * getHeight() / image1.getHeight();
				g.drawLine(0, y, getWidth(), y);
			}
			g.setColor(Color.BLACK);
			g.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());
		}
	}
}
