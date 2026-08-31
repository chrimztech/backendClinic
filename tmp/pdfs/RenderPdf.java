import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

public class RenderPdf {
    public static void main(String[] args) throws Exception {
        try (PDDocument document = Loader.loadPDF(new File(args[0]))) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 144, ImageType.RGB);
                ImageIO.write(image, "png", new File(args[1] + "-" + (page + 1) + ".png"));
            }
        }
    }
}
