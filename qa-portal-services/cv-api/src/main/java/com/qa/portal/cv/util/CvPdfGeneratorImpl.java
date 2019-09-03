package com.qa.portal.cv.util;

import com.qa.portal.common.exception.QaPortalBusinessException;
import com.qa.portal.common.exception.QaPortalSevereException;
import com.qa.portal.cv.domain.CvVersion;
import com.qa.portal.cv.domain.Qualification;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import rst.pdfbox.layout.elements.Document;
import rst.pdfbox.layout.elements.Frame;
import rst.pdfbox.layout.elements.ImageElement;
import rst.pdfbox.layout.elements.Paragraph;
import rst.pdfbox.layout.shape.Stroke;
import rst.pdfbox.layout.text.Alignment;
import rst.pdfbox.layout.text.Position;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class CvPdfGeneratorImpl implements CvPdfGenerator {

    private final Logger LOGGER = LoggerFactory.getLogger(CvPdfGeneratorImpl.class);

    Document document = new Document();

    // QA Colour Scheme
    String QABlue = "#004050";
    String QAPurple = "#7F007D";
    String QARed = "#FF004C";
    String QAGrey = "#565759";

    int pd = 20;
    int sideBarHeaderFontSize = 20;
    int sideBarTitleFontSize = 11;
    int sideBarListsFontSize = 10;
    int titleParagraphSpacing = 4;
    int bodyParagraphFontSize = 9;
    int bodyHeadingsFontSize = 12;

    float heightSideBox1 = document.getPageHeight() / 6 + 4;
    float heightSideBox2 = (float) 2.5 * document.getPageHeight() / 6 + 7;
    float heightSideBox3 = document.getPageHeight() - heightSideBox1 - heightSideBox2;
    float widthCol1 = document.getPageWidth() / 3 + 4.5f;
    float widthCol2 = document.getPageWidth() - widthCol1;
    float heightHeader = document.getPageHeight() / 20 - 8;
    float heightFooter = document.getPageHeight() / 30;
    float heightBody = document.getPageHeight() - heightHeader - heightFooter;

    Frame frame;
    Paragraph paragraph;

    TrueTypeFont montserratTTF;
    TrueTypeFont montserratBoldTTF;
    TrueTypeFont kranaFatBTTF;

    @PostConstruct
    public void createFonts() {
        Resource montRegResource = new ClassPathResource("Montserrat-Regular.ttf");
        Resource montBoldResource = new ClassPathResource("Montserrat-SemiBold.ttf");
        Resource kranaResource = new ClassPathResource("Krana-Fat-B.ttf");
        try {
            this.montserratTTF = new TTFParser().parse(montRegResource.getInputStream());
            this.montserratBoldTTF = new TTFParser().parse(montBoldResource.getInputStream());
            this.kranaFatBTTF = new TTFParser().parse(kranaResource.getInputStream());
        }
        catch (Exception e) {
            throw new QaPortalBusinessException("Cannot load fonts for CV generation");
        }
    }

    @Override
    public byte[] generateCv(CvVersion cvVersion) {
        document = new Document();
        PDDocument pdDocument = document.getPDDocument();
        LOGGER.info(pdDocument.toString());
        try {
            PDFont montserrat = PDType0Font.load(pdDocument, montserratTTF,true);
            PDFont montserratBold = PDType0Font.load(pdDocument, montserratBoldTTF,true);
            PDFont kranaFatB = PDType0Font.load(pdDocument, kranaFatBTTF,true);

            // column 1 box 1
            paragraph = new Paragraph();
            paragraph.addMarkup("{color:#FFFFFF}*" + cvVersion.getFirstName() + "\n" + cvVersion.getSurname() + "*",
                    sideBarHeaderFontSize, kranaFatB, kranaFatB, kranaFatB, kranaFatB);
            paragraph.addMarkup("{color:" + QAPurple + "} \n*Consultant*", sideBarHeaderFontSize, kranaFatB, kranaFatB,
                    kranaFatB, kranaFatB);
            frame = new Frame(paragraph, widthCol1, heightSideBox1);
            frame.setBackgroundColor(Color.decode(QARed));
            frame.setAbsolutePosition(new Position(0, document.getPageHeight()));
            frame.setPadding(pd, pd, pd, pd);
            document.add(frame);

            // column 1 box 1 image
            ImageElement image = new ImageElement("target/classes/Arrow.png");
            image.setWidth(image.getWidth() / 35);
            image.setHeight(image.getHeight() / 35);
            image.setAbsolutePosition(new Position(pd, document.getPageHeight() - heightSideBox1 + pd + 20));
            document.add(image);

            // column 1 box 2
            paragraph = new Paragraph();
            box1_2(montserrat, montserratBold, paragraph, "Programming Languages", cvVersion.getAllSkills().get(0).getProgrammingLanguages());
            box1_2(montserrat, montserratBold, paragraph, "IDEs", cvVersion.getAllSkills().get(0).getIdes());
            box1_2(montserrat, montserratBold, paragraph, "Operating Systems", cvVersion.getAllSkills().get(0).getOperatingSystems());
            box1_2(montserrat, montserratBold, paragraph, "DevOps Technologies", cvVersion.getAllSkills().get(0).getDevops());
            box1_2(montserrat, montserratBold, paragraph, "Database Technologies", cvVersion.getAllSkills().get(0).getDatabases());
            box1_2(montserrat, montserratBold, paragraph, "Project Frameworks", cvVersion.getAllSkills().get(0).getPlatforms());
            box1_2(montserrat, montserratBold, paragraph, "Other", cvVersion.getAllSkills().get(0).getOther());
            frame = new Frame(paragraph, widthCol1, heightSideBox2);
            frame.setBackgroundColor(Color.decode(QAPurple));
            frame.setAbsolutePosition(new Position(0, document.getPageHeight() - heightSideBox1));
            paragraph.setMaxWidth(frame.getWidth() - (pd * 2));
            frame.setPadding(pd, pd, pd, pd);
            document.add(frame);

            // column 1 box 3
            paragraph = new Paragraph();
            paragraph.addMarkup("{color:#FFFFFF}*Qualification*\n", sideBarTitleFontSize, montserrat, montserratBold,
                    montserrat, montserrat);
            paragraph.addMarkup("\n", titleParagraphSpacing, montserrat, montserratBold, montserrat, montserrat);
            for (Qualification i : cvVersion.getAllQualifications()) {
                paragraph.addMarkup("{color:#FFFFFF}" + i.getQualificationDetails(), sideBarListsFontSize, montserrat,
                        montserratBold, montserrat, montserrat);
            }
            frame = new Frame(paragraph, widthCol1, heightSideBox3);
            frame.setBackgroundColor(Color.decode(QABlue));
            frame.setAbsolutePosition(new Position(0, heightSideBox3));
            paragraph.setMaxWidth(frame.getWidth() - (pd * 2));
            frame.setPadding(pd, pd, pd, pd);
            document.add(frame);

            // column 2 Header
            paragraph = new Paragraph();
            paragraph.addMarkup("{color:#89898b}Consultant Profile", 8.8f, montserrat, montserratBold, montserrat,
                    montserrat);
            paragraph.setAlignment(Alignment.Left);
            frame = new Frame(paragraph, widthCol2 - pd * 2, heightHeader);
            frame.setAbsolutePosition(new Position(widthCol1, heightFooter + heightBody + paragraph.getHeight() + 10));
            paragraph.setMaxWidth(frame.getWidth());
            frame.setMargin(pd, 0, 0, 0);
            document.add(frame);

            // column 2 header image
            ImageElement logo = new ImageElement("target/classes/QA_Logo.png");
            logo.setWidth(logo.getWidth() / 37f);
            logo.setHeight(logo.getHeight() / 37f);
            logo.setAbsolutePosition(new Position(widthCol1 + widthCol2 - logo.getWidth() - pd,
                    heightFooter + heightBody + logo.getHeight() + 4));
            document.add(logo);

            // column 2 Body
            paragraph = new Paragraph();
            paragraph.addMarkup("{color:" + QAPurple + "}*PROFILE*\n", bodyHeadingsFontSize, kranaFatB, kranaFatB,
                    kranaFatB, kranaFatB);
            paragraph.addMarkup("\n", 5, kranaFatB, kranaFatB, kranaFatB, kranaFatB);
            // Profile
            paragraph.addMarkup("{color:" + QAGrey + "}" + cvVersion.getProfile().getProfileDetails() + "\n\n\n",
                    bodyParagraphFontSize, montserrat, montserratBold, montserrat, montserrat);
            // Work Experience
            paragraph.addMarkup("{color:" + QAPurple + "}*WORK EXPERIANCE - QA*\n", bodyHeadingsFontSize, kranaFatB,
                    kranaFatB, kranaFatB, kranaFatB);
            paragraph.addMarkup("\n", 5, kranaFatB, kranaFatB, kranaFatB, kranaFatB);
            for (int i = 0; i < cvVersion.getAllWorkExperience().size(); i++) {
                paragraph.addMarkup(
                        "{color:" + QABlue + "}*" + cvVersion.getAllWorkExperience().get(i).getJobTitle() + "*\n",
                        bodyParagraphFontSize, montserrat, montserratBold, montserrat, montserrat);
                paragraph.addMarkup("\n", 4, montserrat, montserratBold, montserrat, montserrat);
                paragraph
                        .addMarkup(
                                "{color:" + QAGrey + "}"
                                        + cvVersion.getAllWorkExperience().get(i).getWorkExperienceDetails() + "\n\n",
                                bodyParagraphFontSize, montserrat, montserratBold, montserrat, montserrat);
            }

            // Hobbies and Interests
            paragraph.addMarkup("{color:" + QAPurple + "}*HOBBIES/INTERESTS*\n", bodyHeadingsFontSize, kranaFatB,
                    kranaFatB, kranaFatB, kranaFatB);
            paragraph.addMarkup("{color:" + QAGrey + "}" + cvVersion.getHobbies().getHobbiesDetails() + "\n\n",
                    bodyParagraphFontSize, montserrat, montserratBold, montserrat, montserrat);
            frame = new Frame(paragraph, widthCol2, heightBody);
            frame.setAbsolutePosition(new Position(widthCol1, document.getPageHeight() - heightHeader));
            paragraph.setMaxWidth(frame.getWidth() - (pd * 2));
            frame.setPadding(pd, pd, pd, pd);
            document.add(frame);

            // column 2 Footer
            paragraph = new Paragraph();
            paragraph.addMarkup("{color:#89898b}+44 1273 022670 / qa.com", 8, montserrat, montserratBold, montserrat,
                    montserrat);
            paragraph.setAlignment(Alignment.Right);
            frame = new Frame(paragraph, widthCol2 - pd * 2, heightHeader);
            frame.setAbsolutePosition(new Position(widthCol1, heightHeader));
            paragraph.setMaxWidth(frame.getWidth());
            frame.setMargin(pd, 0, 0, 0);
            frame.setPaddingTop(5);
            document.add(frame);

            // column 2 divider 1
            paragraph = new Paragraph();
            frame = new Frame(paragraph, widthCol2 - pd * 2, 0.5f);
            divider(frame, document.getPageHeight() - heightHeader);
            document.add(frame);

            // column 2 divider 2
            paragraph = new Paragraph();
            frame = new Frame(paragraph, widthCol2 - pd * 2, 0.5f);
            divider(frame, heightHeader);
            document.add(frame);

            // returns
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            LOGGER.info(document.getPDDocument().toString());

            document.save(out);
            byte[] data = out.toByteArray();
            InputStream in = new ByteArrayInputStream(data);
            byte[] byteArray = IOUtils.toByteArray(in);
            return byteArray;

        } catch (IOException e) {
            e.printStackTrace();
            throw new QaPortalSevereException("Cannot generate pdf");
        }
        finally {
            try {
                pdDocument.close();
                document = null;
            }
            catch (Exception e) {
                LOGGER.info("PD Document could not be closed" + e.getMessage());
            }
        }
    }

    public void divider(Frame frame, float height) {
        frame.setBorder(Color.decode("#d9dbdb"), new Stroke(0.5f));
        frame.setAbsolutePosition(new Position(widthCol1, height));
        frame.setMargin(pd, 0, 0, 0);
        frame.setPaddingTop(5);
    }

    public void box1_2(PDFont montserrat,
                       PDFont montserratBold,
                       Paragraph paragraph,
                       String title,
                       List<String> list) throws IOException {
        paragraph.addMarkup("{color:#FFFFFF}*" + title + "*\n", sideBarTitleFontSize, montserrat, montserratBold,
                montserrat, montserrat);
        paragraph.addMarkup("\n", titleParagraphSpacing, montserrat, montserratBold, montserrat, montserrat);
        for (int i = 0; i < list.size(); i++) {
            if (i < list.size() - 1) {
                paragraph.addMarkup("{color:#FFFFFF}" + list.get(i) + ", ", sideBarListsFontSize, montserrat,
                        montserratBold, montserrat, montserrat);
            } else {
                paragraph.addMarkup("{color:#FFFFFF}" + list.get(i) + "\n\n", sideBarListsFontSize, montserrat,
                        montserratBold, montserrat, montserrat);
            }
        }
    }
}
