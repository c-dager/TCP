import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public static void main(String[] args) throws Exception {
        try (ServerSocketChannel listenChannel = ServerSocketChannel.open()) {
            listenChannel.bind(new InetSocketAddress(3000));
            System.out.println("Server is listening on port 3000...");
            ExecutorService es = Executors.newFixedThreadPool(4);
            String input = "";
            Scanner scanner = new Scanner(System.in);
            while (!input.toLowerCase().equals("q")) {
                System.out.println("Waiting for client connection...");
                SocketChannel serveChannel = listenChannel.accept();
                System.out.println("Client connected: " + serveChannel.getRemoteAddress());

                // Handle client requests in a separate method
                handleClientRequests(serveChannel, es);
            }
            es.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void downloadFileFromClient(SocketChannel channel, String fileName) throws IOException {
       FileOutputStream fileOutputStream = new FileOutputStream("ServerFiles/" + fileName);
       FileChannel fileChannel = fileOutputStream.getChannel();
       ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
       String response;

       try{
           while (channel.read(byteBuffer) > 0){
               byteBuffer.flip();
               fileChannel.write(byteBuffer);
               byteBuffer.clear();
           }
           fileOutputStream.close();
           response = "S";
           ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
           channel.write(responseBuffer);
           channel.close();
       }
       catch (Exception e){
           response = "F";
           ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
           channel.write(responseBuffer);
           byteBuffer.clear();
       }
    }

    static class downloadFileFromClientTask implements Runnable{
        private String fileName;
        private SocketChannel channel;
        public downloadFileFromClientTask(SocketChannel channel, String fileName){
            this.channel = channel;
            this.fileName = fileName;
        }
        public void run(){
            try {
                downloadFileFromClient(channel, fileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void renameFile(SocketChannel channel, String fileName, String newFileName) throws IOException {

        File orignalFile = new File("ServerFiles", fileName);
        File newFile = new File("ServerFiles",newFileName);
        if (newFile.exists()) {
            System.out.println("Failed to find file");
            ByteBuffer responseBuffer = ByteBuffer.wrap("F".getBytes());
            channel.write(responseBuffer);// Failure
            return;
        }
        System.out.println("Renaming file from " + orignalFile.getName() + " to " + newFile.getName());
        boolean renamed = orignalFile.renameTo(newFile);
        ByteBuffer responseBuffer;
        if (renamed) {
            responseBuffer = ByteBuffer.wrap("S".getBytes()); // Success
        } else {
            responseBuffer = ByteBuffer.wrap("F".getBytes()); // Failure
        }
        channel.write(responseBuffer);

    }

    static class renameFileTask implements Runnable{
        private String oldFileName;
        private String newFileName;
        private SocketChannel channel;
        public renameFileTask(SocketChannel channel, String oldFileName, String newFileName){
            this.channel = channel;
            this.oldFileName = oldFileName;
            this.newFileName = newFileName;
        }
        public void run(){
            try {
                renameFile(channel, oldFileName, newFileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void deleteFile(SocketChannel channel, String fileName) throws IOException {
        Path filePath = Paths.get("ServerFiles", fileName);
        System.out.println(filePath.getFileName());

        // Attempt to delete the file
        try {
            boolean deleted = Files.deleteIfExists(filePath);
            // Prepare the response
            ByteBuffer responseBuffer;
            if (deleted) {
                responseBuffer = ByteBuffer.wrap("S".getBytes()); // Success
            } else {
                responseBuffer = ByteBuffer.wrap("F".getBytes()); // Failure
            }
            channel.write(responseBuffer);
        } catch (IOException e) {
            // Handle any exceptions during file deletion
            e.printStackTrace();
            ByteBuffer responseBuffer = ByteBuffer.wrap("F".getBytes()); // Failure
            channel.write(responseBuffer);
        }
    }

    static class deleteFileTask implements Runnable{
        private String fileName;
        private SocketChannel channel;
        public deleteFileTask(SocketChannel channel, String fileName){
            this.channel = channel;
            this.fileName = fileName;
        }
        public void run(){
            try {
                deleteFile(channel, fileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void listFiles(SocketChannel clientChannel) throws IOException {
        System.out.println("Requesting file list...");

        // Get the list of files in the specified directory
        Path dirPath = Paths.get("ServerFiles");
        List<Path> fileList = Files.list(dirPath).toList();

        // Create a response string
        StringBuilder response = new StringBuilder("FILE_LIST:");
        for (Path file : fileList) {
            response.append(file.getFileName()).append(","); // Append file names
        }

        // Remove the last comma if there are files
        if (!fileList.isEmpty()) {
            response.setLength(response.length() - 1); // Remove the last comma
        }

        // Send the response to the client
        ByteBuffer responseBuffer = ByteBuffer.wrap(response.toString().getBytes());
        clientChannel.write(responseBuffer);
        System.out.println("Sent file list to client: " + response);
    }
    static class listFilesTask implements Runnable{
        private SocketChannel channel;
        public listFilesTask(SocketChannel channel){
            this.channel = channel;
        }
        public void run(){
            try {
                listFiles(channel);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private static void uploadFileToClient(SocketChannel serveChannel, String fileName) throws IOException {
        System.out.println("File name message: " + fileName);

        File file = new File("ServerFiles/" + fileName);
        if (!file.exists()) {
            System.out.println("File doesn't exist");
            return; // Exit the method if the file doesn't exist
        }
        System.out.println(file.toPath());
        try (FileInputStream fileInputStream = new FileInputStream(file);
             FileChannel fileChannel = fileInputStream.getChannel()) {
            ByteBuffer fileContent = ByteBuffer.allocate(1024);
            int bytesRead;
            // Read the file content and send it to the client
            while ((bytesRead = fileChannel.read(fileContent)) > 0) {
                fileContent.flip(); // Prepare the buffer for writing
                while (fileContent.hasRemaining()) {
                    int written = serveChannel.write(fileContent); // Write to the socket channel
                    if(written == 0){
                        System.out.println("Channel not ready for writing, waiting");
                    }
                }
                fileContent.clear(); // Clear the buffer for the next read
            }

            // Check if we reached the end of the file
            if (bytesRead == -1) {
                System.out.println("End of file reached, upload completed.");
            } else {
                System.out.println("File upload completed, but not all data was read.");
            }
            serveChannel.close();

        } catch (IOException e) {
            System.err.println("Error during file upload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static class uploadFileToClientTask implements Runnable{
        private String fileName;
        private SocketChannel channel;
        public uploadFileToClientTask(SocketChannel channel, String fileName){
            this.channel = channel;
            this.fileName = fileName;
        }
        public void run(){
            try {
                uploadFileToClient(channel, fileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void handleClientRequests(SocketChannel serveChannel, ExecutorService es) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
                int bytesRead = serveChannel.read(buffer);

                if (bytesRead == -1) {
                    System.out.println("Client disconnected.");
                    return; // Exit the loop if the client disconnects
                }

                buffer.flip();
                byte[] requestBytes = new byte[bytesRead];
                buffer.get(requestBytes);
                String clientRequest = new String(requestBytes);
                String[] clientRequestList = clientRequest.split("\\|");
                String action = clientRequestList[0];

                switch (action) {
                    case "LIST":
                        listFiles(serveChannel);
                        //es.submit(new listFilesTask(serveChannel));
                        break;
                    case "DELETE":
                        deleteFile(serveChannel, clientRequestList[1]);
                        //es.submit(new deleteFileTask(serveChannel, clientRequestList[1]));
                        break;
                    case "RENAME":
                        renameFile(serveChannel, clientRequestList[1], clientRequestList[2]);
                        //es.submit(new renameFileTask(serveChannel, clientRequestList[1], clientRequestList[2]));
                        break;
                    case "DOWNLOAD":
                        //uploadFileToClient(serveChannel, clientRequestList[1]);
                        es.submit(new uploadFileToClientTask(serveChannel, clientRequestList[1]));
                        break;
                    case "UPLOAD":
                        //downloadFileFromClient(serveChannel, clientRequestList[1]);
                        es.submit(new downloadFileFromClientTask(serveChannel, clientRequestList[1]));
                        break;
                    default:
                        System.out.println("Invalid command, try again");
                }

                // Clear the buffer for the next read
                buffer.clear();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                serveChannel.close(); // Ensure the channel is closed when done
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
