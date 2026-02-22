package com.sanjay.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class RegexXmlCleanerApp extends JFrame {

    private final JTextField inputPath = new JTextField();
    private final JTextField outputPath = new JTextField();

    private final JTextArea regexFind = new JTextArea(3, 40);
    private final JTextArea regexReplace = new JTextArea(3, 40);

    private final JTextArea preview = new JTextArea(18, 70);

    private final JCheckBox keepBr = new JCheckBox("Keep <br/>", true);
    private final JCheckBox dropNonP = new JCheckBox("Drop non-<p> content (tables/images/div etc.)", true);

    public RegexXmlCleanerApp() {
        super("Regex → Clean → XML Output");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        root.add(buildTopPanel(), BorderLayout.NORTH);
        root.add(buildCenterPanel(), BorderLayout.CENTER);
        root.add(buildBottomPanel(), BorderLayout.SOUTH);

        preview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        preview.setLineWrap(true);
        preview.setWrapStyleWord(true);

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildTopPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Input
        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        p.add(new JLabel("Input file:"), c);

        c.gridx = 1; c.weightx = 1;
        p.add(inputPath, c);

        c.gridx = 2; c.weightx = 0;
        p.add(new JButton(new AbstractAction("Browse") {
            @Override public void actionPerformed(ActionEvent e) { chooseFile(inputPath, false); }
        }), c);

        // Output
        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        p.add(new JLabel("Output XML:"), c);

        c.gridx = 1; c.weightx = 1;
        p.add(outputPath, c);

        c.gridx = 2; c.weightx = 0;
        p.add(new JButton(new AbstractAction("Save As") {
            @Override public void actionPerformed(ActionEvent e) { chooseFile(outputPath, true); }
        }), c);

        return p;
    }

    private JPanel buildCenterPanel() {
        JPanel p = new JPanel(new BorderLayout(12, 12));

        JPanel regexPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        regexPanel.add(labeledScroll("Regex Find (Java regex):", regexFind));
        regexPanel.add(labeledScroll("Regex Replace:", regexReplace));

        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT));
        options.add(keepBr);
        options.add(dropNonP);

        JPanel top = new JPanel(new BorderLayout());
        top.add(regexPanel, BorderLayout.CENTER);
        top.add(options, BorderLayout.SOUTH);

        p.add(top, BorderLayout.NORTH);
        p.add(labeledScroll("Preview (clean XML):", preview), BorderLayout.CENTER);

        return p;
    }

    private JPanel buildBottomPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        p.add(new JButton(new AbstractAction("Preview") {
            @Override public void actionPerformed(ActionEvent e) { run(false); }
        }));

        p.add(new JButton(new AbstractAction("Generate XML") {
            @Override public void actionPerformed(ActionEvent e) { run(true); }
        }));

        return p;
    }

    private JScrollPane labeledScroll(String label, JTextArea area) {
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createTitledBorder(label));
        return sp;
    }

    private void chooseFile(JTextField target, boolean saveDialog) {
        JFileChooser fc = new JFileChooser();
        int r = saveDialog ? fc.showSaveDialog(this) : fc.showOpenDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            target.setText(fc.getSelectedFile().getAbsolutePath());
            if (!saveDialog && outputPath.getText().isBlank()) {
                targetToOutputGuess(fc.getSelectedFile());
            }
        }
    }

    private void targetToOutputGuess(File in) {
        String name = in.getName();
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;
        File out = new File(in.getParentFile(), base + "_CLEAN.xml");
        outputPath.setText(out.getAbsolutePath());
    }

    private void run(boolean writeFile) {
        try {
            String inPath = inputPath.getText().trim();
            if (inPath.isEmpty()) throw new IllegalArgumentException("Select an input file.");
            File inFile = new File(inPath);
            if (!inFile.exists()) throw new FileNotFoundException("Input not found: " + inPath);

            String raw = readAsText(inFile);

            // 1) Regex step (user-defined)
            String find = regexFind.getText();
            if (find != null && !find.isBlank()) {
                Pattern p = Pattern.compile(find, Pattern.DOTALL);
                raw = p.matcher(raw).replaceAll(regexReplace.getText());
            }

            // 2) Clean → XML with only p/b/i (+ optional br)
            String cleaned = cleanToXml(raw, keepBr.isSelected(), dropNonP.isSelected());

            preview.setText(cleaned);

            if (writeFile) {
                String outPath = outputPath.getText().trim();
                if (outPath.isEmpty()) throw new IllegalArgumentException("Select output file.");
                Files.writeString(new File(outPath).toPath(), cleaned, StandardCharsets.UTF_8);
                JOptionPane.showMessageDialog(this, "Saved:\n" + outPath, "Done", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String readAsText(File f) throws Exception {
        String name = f.getName().toLowerCase();

        if (name.endsWith(".txt") || name.endsWith(".xml") || name.endsWith(".html") || name.endsWith(".htm")) {
            return Files.readString(f.toPath(), StandardCharsets.UTF_8);
        }

        if (name.endsWith(".docx")) {
            try (InputStream is = new FileInputStream(f); XWPFDocument doc = new XWPFDocument(is)) {
                // Convert DOCX paragraphs into simple HTML-like text (we'll wrap to <p>)
                StringBuilder sb = new StringBuilder();
                doc.getParagraphs().forEach(par -> {
                    String t = par.getText();
                    if (t != null && !t.isBlank()) sb.append("<p>").append(escapeXml(t)).append("</p>\n");
                });
                return sb.toString();
            }
        }

        if (name.endsWith(".doc")) {
            try (InputStream is = new FileInputStream(f); HWPFDocument doc = new HWPFDocument(is)) {
                WordExtractor extractor = new WordExtractor(doc);
                String[] paras = extractor.getParagraphText();
                StringBuilder sb = new StringBuilder();
                for (String para : paras) {
                    if (para != null) {
                        String t = para.strip();
                        if (!t.isBlank()) sb.append("<p>").append(escapeXml(t)).append("</p>\n");
                    }
                }
                return sb.toString();
            }
        }

        throw new IllegalArgumentException("Unsupported file type: " + f.getName());
    }

    private static String cleanToXml(String input, boolean keepBr, boolean dropNonP) {
        // Parse as HTML (handles messy HTML safely)
        Document doc = Jsoup.parse(input);

        // Convert known "bold" class spans to <b>
        // Example in your file: <span class="body-text-bold">Narrator</span> :contentReference[oaicite:2]{index=2}
        Elements boldSpans = doc.select("span[class~=.*bold.*]");
        for (Element s : boldSpans) {
            s.tagName("b");
            s.removeAttr("class");
        }

        // Convert known "italic" class spans to <i> (if present in other files)
        Elements italicSpans = doc.select("span[class~=.*italic.*]");
        for (Element s : italicSpans) {
            s.tagName("i");
            s.removeAttr("class");
        }

        // Remove ALL attributes everywhere (class/id/style etc.)
        for (Element el : doc.getAllElements()) {
            el.clearAttributes();
        }

        // We will build a clean XML document: <document><p>...</p>...</document>
        Document out = Document.createShell("");
        out.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        out.outputSettings().charset(StandardCharsets.UTF_8);
        out.outputSettings().prettyPrint(true);

        Element root = out.body().appendElement("document");

        // Grab paragraph-like content
        List<Element> paragraphs = doc.select("p");

        for (Element p : paragraphs) {
            Element cleanP = new Element("p");

            // Keep only text + b + i + (optional) br inside p
            // Anything else gets "unwrapped" into text.
            sanitizeChildren(p, keepBr);

            // After sanitizing, copy p’s child nodes into cleanP
            for (Node n : p.childNodes()) {
                cleanP.appendChild(n.clone());
            }

            // If paragraph becomes empty, skip
            if (!cleanP.text().isBlank() || cleanP.select("b,i").size() > 0) {
                root.appendChild(cleanP);
            }
        }

        if (!dropNonP) {
            // Optional: if you want to keep stray text not inside <p>, you can add it here.
            // (default is drop)
        }

        // Return only the XML content (without HTML wrapper)
        // out.outerHtml() includes <html><head>...; we want just root
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + root.outerHtml() + "\n";
    }

    private static void sanitizeChildren(Element p, boolean keepBr) {
        // Walk through descendants; allow only b,i,br,text inside <p>
        for (Element child : p.select("*")) {
            String tag = child.tagName();
            boolean allowed = tag.equals("b") || tag.equals("i") || (keepBr && tag.equals("br"));

            if (!allowed) {
                // Replace unknown tag with its text/children (unwrap)
                child.unwrap();
            }
        }
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegexXmlCleanerApp().setVisible(true));
    }
}