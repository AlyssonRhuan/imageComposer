package com.arcs.image.composer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application
{
    public static final String BASE_FOLDER_PATH = "C:/Users/ecaraly/Documents/image.composer/";
    public static final String IMAGES_FOLDER_PATH = BASE_FOLDER_PATH + "images/";

    public static final String PAGE_URL = "https://jovemnerd.com.br/";

    public static final String PDF = "COMPOSER.pdf";
    public static final String MOBI = "COMPOSER.mobi";

    public static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws IOException
    {
        LOG.info("Starting applciation");
        SpringApplication.run(Application.class, args);

        if (validations())
        {
            ArrayList<String> imagesUrl = listAllImages();

            int imageIndex = 1;

            for (String imageUrl : imagesUrl)
            {
                String imageName = getImageName(imageUrl);
                imageImport(imageUrl, imageName, imageIndex);
                imageIndex++;
            }

            // abrir pasta para deletar imagens que não fazem parte

            composePdf();

//            clearFolder();
        }
    }

    public static void clearFolder()
    {
        LOG.info("Deleting folder.");
        File folder = new File(BASE_FOLDER_PATH);
        for (String image : folder.list())
        {
            File currentFile = new File(folder.getPath(), image);
            currentFile.delete();
        }
        folder.delete();
    }

    public static boolean validations()
    {
        LOG.info("Starting validations");
        try
        {
            if (!Jsoup.connect(PAGE_URL).get().hasText())
            {
                LOG.error("URL Page not found.");
                return false;
            }

            if (!new File(BASE_FOLDER_PATH).isDirectory())
            {
                LOG.error("Folder doesn't not exist.");
                new File(BASE_FOLDER_PATH).mkdir();
                LOG.error("Folder created.");
            }

            if (!new File(IMAGES_FOLDER_PATH).isDirectory())
            {
                LOG.error("Folder doesn't not exist.");
                new File(IMAGES_FOLDER_PATH).mkdir();
                LOG.error("Folder created.");
            }

            if (!new File(BASE_FOLDER_PATH).canWrite())
            {
                LOG.error("Folder access denied.");
                return false;
            }
        }
        catch (Exception ex)
        {
            LOG.error(ex.getMessage());
            return false;
        }

        return true;
    }

    public static String getImageName(String imageUrl)
    {
        LOG.info("Method getImageName");

        String imageName = "";

        String[] urlSplit = imageUrl.split("/");

        return urlSplit[urlSplit.length - 1];
    }

    public static ArrayList<String> listAllImages()
    {
        LOG.info("Method listAllImages");
        ArrayList<String> urls = new ArrayList<>();
        try
        {
            Document doc = Jsoup.connect(PAGE_URL).get();
            doc.select("img").forEach(element -> urls.add(element.attr("src")));
        }
        catch (Exception e)
        {
            urls.clear();
        }

        return urls;
    }

    public static boolean imageImport(String imageUrl, String imageName, int imageIndex)
    {
        LOG.info("Method imageImport");

        OutputStream outputStream = null;

        try
        {
            LOG.info("Image " + imageUrl);

            URL urlObj = new URL(imageUrl);
            HttpURLConnection httpConnection = (HttpURLConnection) urlObj.openConnection();
            httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
            httpConnection.setRequestMethod("GET");
            InputStream inputStream = httpConnection.getInputStream();
            int read = 0;
            byte[] bytes = new byte[1024];
            File file = new File(IMAGES_FOLDER_PATH + imageIndex + "_" + imageName);
            outputStream = new FileOutputStream(file);
            while ((read = inputStream.read(bytes)) != -1)
            {
                outputStream.write(bytes, 0, read);
            }
        }
        catch (Exception ex)
        {
            LOG.error(ex.getMessage());
        }
        finally
        {
            try
            {
                if (outputStream != null)
                {
                    outputStream.close();
                }
            }
            catch (IOException ex)
            {
                LOG.error(ex.getMessage());
            }
        }

        return false;
    }

    public static boolean composePdf()
    {
        try
        {
            PDDocument document = new PDDocument();
            PDPage blankPage = null;

            File folder = new File(IMAGES_FOLDER_PATH);
            for (String image : folder.list())
            {
                PDImageXObject pdImage = PDImageXObject.createFromFile(IMAGES_FOLDER_PATH + image, document);

                blankPage = new PDPage(new PDRectangle(pdImage.getWidth() + 50, pdImage.getHeight() + 50));

                PDPageContentStream contentStream = new PDPageContentStream(document, blankPage);
                contentStream.drawImage(pdImage, 25, 25);
                contentStream.close();

                document.addPage(blankPage);
            }

            document.save(BASE_FOLDER_PATH + PDF);
            document.save(BASE_FOLDER_PATH + MOBI);
            document.close();
        }
        catch (Exception ex)
        {
            LOG.error(ex.getMessage());
        }

        return false;
    }
}
