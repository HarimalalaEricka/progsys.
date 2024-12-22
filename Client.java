package client;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Properties;
import fichier.*;

public class Client {
    private static String SERVER_ADDRESS;
    private static int SERVER_PORT;

    private static Socket socket;
    private static DataInputStream in;
    private static DataOutputStream out;

    public static void main(String[] args) {
        loadConfig();

        SwingUtilities.invokeLater(Client::createAndShowGUI);
    }

    private static void loadConfig() {
        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream("configuration/config.txt")) {
            properties.load(input);

            // Lire les valeurs du fichier de configuration
            SERVER_ADDRESS = properties.getProperty("SERVER_ADDRESS");
            SERVER_PORT = Integer.parseInt(properties.getProperty("PORT"));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("Erreur de format dans le fichier de configuration.");
        }
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Client de Fichiers");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); 
        frame.setSize(600, 400);

        JPanel panel = new JPanel(new BorderLayout());

        DefaultListModel<String> fileListModel = new DefaultListModel<>();
        JList<String> fileList = new JList<>(fileListModel);
        JScrollPane scrollPane = new JScrollPane(fileList);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 4));
        JButton uploadButton = new JButton("Uploader");
        JButton downloadButton = new JButton("Downloader");
        JButton deleteButton = new JButton("Supprimer");
        JButton refreshButton = new JButton("Actualiser");
        buttonPanel.add(uploadButton);
        buttonPanel.add(downloadButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        frame.add(panel);

        frame.setVisible(true);

        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    try {
                        if (socket != null && !socket.isClosed()) {
                            out.writeUTF("EXIT");
                            socket.close(); 
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    System.exit(0); 
                }
            });

            refreshButton.addActionListener(e -> refreshFileList(out, in, fileListModel));

            downloadButton.addActionListener(e -> {
                String selectedFile = fileList.getSelectedValue();
                if (selectedFile != null) {
                    downloadFile(out, in, selectedFile);
                } else {
                    JOptionPane.showMessageDialog(frame, "Veuillez sélectionner un fichier à télécharger.");
                }
            });

            deleteButton.addActionListener(e -> {
                String selectedFile = fileList.getSelectedValue();
                if (selectedFile != null) {
                    deleteFile(out, in, selectedFile);
                } else {
                    JOptionPane.showMessageDialog(frame, "Veuillez sélectionner un fichier à supprimer.");
                }
            });

            uploadButton.addActionListener(e -> uploadFile(out, in));

            refreshFileList(out, in, fileListModel);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void refreshFileList(DataOutputStream out, DataInputStream in, DefaultListModel<String> fileListModel) {
        try {
            out.writeUTF("LIST");
            String response = in.readUTF();

            if ("OK".equals(response)) {
                int fileCount = in.readInt();
                fileListModel.clear();

                for (int i = 0; i < fileCount; i++) {
                    fileListModel.addElement(in.readUTF());
                }
            } else {
                JOptionPane.showMessageDialog(null, response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void downloadFile(DataOutputStream out, DataInputStream in, String fileName) {
        try {
            out.writeUTF("DOWNLOAD");
            out.writeUTF(fileName);
            FichierProject fiche = new FichierProject();
            String fichier = fiche.RassemblerFichier(fileName);
            String fichier2 = fiche.renommerFichier(fichier, fileName);
            System.out.println(fichier2);
            File localFile = new File(fichier2);
            long fileSize = localFile.length(); 
            String response = in.readUTF();
                System.out.println(fileSize);
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new File(fichier2));
                fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
                int result = fileChooser.showSaveDialog(null);

                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    System.out.println(file);
                        FileInputStream fch = new FileInputStream(fichier2);

                    try (FileOutputStream fileOut = new FileOutputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while (fileSize > 0 && (bytesRead = fch.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                            fileOut.write(buffer, 0, bytesRead);
                            fileSize -= bytesRead;
                            // System.out.println(fileSize);
                        }
                    }
                fiche.supprimerFichier(fichier2);
                    JOptionPane.showMessageDialog(null, "Téléchargement terminé");
                }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteFile(DataOutputStream out, DataInputStream in, String fileName) {
        try {
            out.writeUTF("DELETE");
            out.writeUTF(fileName);

            String response = in.readUTF();
            JOptionPane.showMessageDialog(null, "Fichier supprimé");
            

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void uploadFile(DataOutputStream out, DataInputStream in) {
        try {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
  
                out.writeUTF("UPLOAD");
                out.writeUTF(file.getName());
                out.writeLong(file.length());

                try (FileInputStream fileIn = new FileInputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileIn.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                String response = in.readUTF();
                JOptionPane.showMessageDialog(null, response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
