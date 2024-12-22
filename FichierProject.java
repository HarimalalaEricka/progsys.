package fichier;
import java.net.*;
import java.io.*;
public class FichierProject
{
    public boolean creerDossier(String nomDossier) {
        File dossier = new File(nomDossier);
        
        if (dossier.exists()) {
            System.out.println("Erreur : Le dossier " + nomDossier + " existe déjà.");
            return false;
        }
        
        boolean created = dossier.mkdir();  
        if (created) {
            System.out.println("Le dossier " + nomDossier + " a été créé avec succès.");
        } else {
            System.out.println("Erreur lors de la création du dossier.");
        }
        
        return created;
    }
public void DiviserFichier(String sourceFile) {
    String p1 = sourceFile + "-1.ext";
    String p2 = sourceFile + "-2.ext";
    String p3 = sourceFile + "-3.ext";
    String p4 = sourceFile + "-4.ext";
    String[] outputFiles = {p1, p2, p3, p4};  

    try (FileInputStream inputStream = new FileInputStream(sourceFile)) {
        long totalSize = inputStream.available();
        long partSize = (totalSize + outputFiles.length - 1) / outputFiles.length; 
        String filePath = "configuration/serveur.txt"; 
        String[] config = new String[(outputFiles.length*3)]; 
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            for (int i = 0; i < outputFiles.length*3; i++) {
                config[i] = reader.readLine();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier : " + e.getMessage());
        }

        byte[] allBytes = inputStream.readAllBytes();
        
        int currentPosition = 0;
        for ( int i = 0; i < outputFiles.length; i++) {
            int bytesToWrite = (int) Math.min(partSize, allBytes.length - currentPosition);
            
            try (FileOutputStream outputStream = new FileOutputStream(outputFiles[i])) {
                outputStream.write(allBytes, currentPosition, bytesToWrite);
                currentPosition += bytesToWrite;
            }
            UploadViaServeur(outputFiles[i], config[i*3], config[(i*3)+1], config[(i*3)+2]);
        }
        System.out.println("Fichier divisé et transféré avec succès.");
        for( String p: outputFiles)
        {
            supprimerFichier(p);
        }
    } catch (IOException e) {
        System.err.println("Erreur : " + e.getMessage());
    }
}

private void UploadViaServeur(String fileName, String server, String user, String pass) {
    String remotePath = "/Reseaux/" + fileName; 
    try {
        URL url = new URL("ftp://" + user + ":" + pass + "@" + server + remotePath + ";type=i");

        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);

        try (OutputStream outputStream = connection.getOutputStream();
             FileInputStream fileInputStream = new FileInputStream(fileName)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            System.out.println("Fichier " + fileName + " transféré avec succès.");
        }

    } catch (IOException e) {
        System.err.println("Erreur lors du transfert FTP pour " + fileName + ": " + e.getMessage());
        e.printStackTrace();
    }
} 

    public String RassemblerFichier(String sourceFile) {
    String outputFile = "reassembled.ext"; 
    String[] remoteFiles = {
       sourceFile + "-1.ext",
       sourceFile + "-2.ext",
       sourceFile + "-3.ext",
       sourceFile + "-4.ext"
    };

    String[] config = new String[(remoteFiles.length*3)]; 
        String filePath = "configuration/serveur.txt"; 
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            for (int i = 0; i < remoteFiles.length*3; i++) {
                config[i] = reader.readLine();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier : " + e.getMessage());
        }                  

    try {
        for (int i = 0; i < remoteFiles.length; i++) {
            downloadFromServeur(config[i*3], config[(i*3)+1], config[(i*3)+2], remoteFiles[i]);
        }

        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            for (String localFile : remoteFiles) {
                try (FileInputStream inputStream = new FileInputStream(localFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            }

            System.out.println("Fichiers rassemblés avec succès en " + outputFile);
        }
        for( String r : remoteFiles)
        {
            supprimerFichier(r);
        }
    } catch (IOException e) {
        System.err.println("Erreur : " + e.getMessage());
    }

    return outputFile;
}

public void downloadFromServeur(String server, String user, String pass, String remoteFile) {
    try {
        String remotePath = "/Reseaux/" + remoteFile; 
        URL url = new URL("ftp://" + user + ":" + pass + "@" + server + remotePath + ";type=i");

        URLConnection connection = url.openConnection();
        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(remoteFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            System.out.println("Fichier " + remoteFile + " téléchargé avec succès en tant que " + remoteFile);
        }

    } catch (IOException e) {
        System.err.println("Erreur lors du téléchargement FTP pour " + remoteFile + ": " + e.getMessage());
        e.printStackTrace();
    }
}
 public static void deleteFileFromServeur(String server, String user, String pass, String fileToDelete) {
        try {
            String remoteDirectory = "/Reseaux/";
            Socket socket = new Socket(server, 21); 
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            System.out.println("Serveur : " + reader.readLine());

            writer.println("USER " + user);
            System.out.println("Serveur : " + reader.readLine());

            writer.println("PASS " + pass);
            System.out.println("Serveur : " + reader.readLine());

            writer.println("CWD " + remoteDirectory);
            System.out.println("Serveur : " + reader.readLine());

            writer.println("DELE " + fileToDelete);
            String response = reader.readLine();
            if (response.startsWith("250")) {
                System.out.println("Le fichier " + fileToDelete + " a été supprimé avec succès.");
            } else {
                System.out.println("Erreur lors de la suppression : " + response);
            }

            writer.println("QUIT");
            System.out.println("Serveur : " + reader.readLine());

            reader.close();
            writer.close();
            socket.close();

        } catch (IOException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    public String renommerFichier(String oldFileName, String newFileName) 
    {
        File oldFile = new File(oldFileName);
        File newFile = new File(newFileName);
        boolean a ;
        if (!oldFile.exists()) {
            System.out.println("Erreur : Le fichier " + oldFileName + " n'existe pas.");
            a = false;
        }
        if (newFile.exists()) {
            System.out.println("Erreur : Un fichier avec le nom " + newFileName + " existe déjà.");
            a = false;
        }
        boolean renamed = oldFile.renameTo(newFile);
        if (renamed)
        {
            System.out.println("Le fichier a été renommé avec succès en : " + newFileName);
        } else 
        {
            System.out.println("Erreur lors du renommage du fichier.");
        }
        return newFileName;
    }
        public static boolean supprimerFichier(String fileName) {
        File file = new File(fileName);

        if (!file.exists()) {
            System.out.println("Erreur : Le fichier " + fileName + " n'existe pas.");
            return false;
        }

        boolean deleted = file.delete();
        if (deleted) {
            System.out.println("Le fichier " + fileName + " a été supprimé avec succès.");
        } else {
            System.out.println("Erreur : Impossible de supprimer le fichier " + fileName + ".");
        }
        return deleted;
    }

}