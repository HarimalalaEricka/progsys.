package serveur;
import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.concurrent.*;
import fichier.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Serveur {

    private static int PORT;
    private static String STORAGE_DIR;
    private static Connection CONNEXION;

    public static void main(String[] args) {
        loadConfig();

        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur démarré sur le port " + PORT);
            new File(STORAGE_DIR).mkdir();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connecté : " + clientSocket.getInetAddress());
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Erreur du serveur : " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }

    private static void loadConfig() {
        Properties properties = new Properties();
        
        try (FileInputStream input = new FileInputStream("configuration/config.txt")) {
            properties.load(input);

            PORT = Integer.parseInt(properties.getProperty("PORT"));
            STORAGE_DIR = properties.getProperty("STORAGE_DIR");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.err.println("Erreur de format dans le fichier de configuration.");
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream in = new DataInputStream(socket.getInputStream());
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

                while (true) {
                    String action = in.readUTF();

                    switch (action) {
                        case "UPLOAD":
                            receiveFile(in, out);
                            break;
                        case "DOWNLOAD":
                            sendFile(in, out);
                            break;
                        case "DELETE":
                            deleteFile(in, out);
                            break;
                        case "LIST":
                            listFiles(out);
                            break;
                        case "EXIT":
                            System.out.println("Client déconnecté.");
                            return;
                        default:
                            out.writeUTF("Action inconnue");
                    }
                }
            } catch (IOException e) {
                System.err.println("Erreur avec le client : " + e.getMessage());
            }
        }

            public static void insertFileName(String fileName) {
        String url = "jdbc:mysql://localhost:3306/ftp";
        String user = "root";
        String password = "";

        String sql = "INSERT INTO ftp (nomFichier) VALUES (?)";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fileName);

            int rowsInserted = pstmt.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("Le fichier a été inséré avec succès !");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion : " + e.getMessage());
        }
    }

        private void receiveFile(DataInputStream in, DataOutputStream out) throws IOException {
            String fileName = in.readUTF();
            insertFileName( fileName);
            File file = new File(STORAGE_DIR, fileName);
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                long fileSize = in.readLong();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while (fileSize > 0 && (bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                    fileSize -= bytesRead;
                }
            }
            out.writeUTF("Upload terminé");
            FichierProject fichier = new FichierProject();
            fichier.DiviserFichier(fileName);
            fichier.supprimerFichier(fileName);
        }


        private void sendFile(DataInputStream in, DataOutputStream out) throws IOException {
    String fileName = in.readUTF(); 
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ftp", "root", "");
        
        String sql = "SELECT nomFichier FROM ftp WHERE nomfichier = ?";
        stmt = conn.prepareStatement(sql);
        stmt.setString(1, fileName);
        rs = stmt.executeQuery();

        if (rs.next()) {
            byte[] fileContent = rs.getBytes("nomFichier");

            out.writeUTF("OK");
            out.writeLong(fileContent.length);

            out.write(fileContent);
        } else {
            out.writeUTF("Fichier introuvable");
        }
    } catch (SQLException e) {
        e.printStackTrace();
        out.writeUTF("Erreur de base de données");
    } finally {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

public static void deleteFileName(String fileName) {
        String url = "jdbc:mysql://localhost:3306/ftp";
        String user = "root";
        String password = "";

        String sql = "DELETE FROM ftp WHERE nomFichier = ?";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fileName);

            int rowsInserted = pstmt.executeUpdate();

            if (rowsInserted > 0) {
                System.out.println("Le fichier a été effaceé avec succès !");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion : " + e.getMessage());
        }
    }
        private void deleteFile(DataInputStream in, DataOutputStream out) throws IOException {
            String fileName = in.readUTF();
            String p1 = fileName + "-1.ext";
            String p2 = fileName + "-2.ext";
            String p3 = fileName + "-3.ext";
            String p4 = fileName + "-4.ext";
            String[] outputFiles = {p1, p2, p3, p4};  
            String filePath = "configuration/serveur.txt"; 

            String[] config = new String[(outputFiles.length*3)]; 
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                for (int i = 0; i < outputFiles.length*3; i++) {
                    config[i] = reader.readLine();
                }
            } catch (IOException e) {
                System.err.println("Erreur lors de la lecture du fichier : " + e.getMessage());
            }
            FichierProject fiche = new FichierProject();
            for(int i = 0; i < outputFiles.length; i++)
            {
                fiche.deleteFileFromServeur(config[i*3], config[(i*3)+1], config[(i*3)+2], outputFiles[i]);
            }
            deleteFileName(fileName);
            out.writeUTF("Fichier supprimé");
        }


        private void listFiles(DataOutputStream out) throws IOException {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

    try {
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ftp", "root", "");
        stmt = conn.createStatement();
        
        String sql = "SELECT nomFichier FROM ftp";
        rs = stmt.executeQuery(sql);
        
        List<String> fileNames = new ArrayList<>();
        while (rs.next()) {
            fileNames.add(rs.getString("nomfichier"));
        }
        
        if (!fileNames.isEmpty()) {
            out.writeUTF("OK"); 
            out.writeInt(fileNames.size()); 
            
            for (String fileName : fileNames) {
                out.writeUTF(fileName);
            }
        } else {
            out.writeUTF("Aucun fichier disponible");
        }
    } catch (SQLException e) {
        e.printStackTrace();
        try {
            out.writeUTF("Erreur de base de données");
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    } finally {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

    }
}
