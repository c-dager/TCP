import java.io.File;
import java.io.FileInputStream;
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

public class Server {
    public static void main(String[] args) throws Exception {
        try (ServerSocketChannel listenChannel = ServerSocketChannel.open()) {
            listenChannel.bind(new InetSocketAddress(3000));
            System.out.println("Server is listening on port 3000...");

            while (true) {
                System.out.println("Waiting for client connection...");
                SocketChannel serveChannel = listenChannel.accept();
                System.out.println("Client connected: " + serveChannel.getRemoteAddress());

                // Handle client requests in a separate method
                handleClientRequests(serveChannel);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void downloadFileFromClient() {
    }

    private static void renameFile() {

    }

    private static void deleteFile() {

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


    //TODO: ALLOW THIS TO BE CALLED TWICE IN A ROW
    private static void uploadFileToClient(SocketChannel serveChannel, String fileName) throws IOException {
        System.out.println("File name message: " + fileName);

        File file = new File("ServerFiles/" + fileName);
        if (!file.exists()) {
            System.out.println("File doesn't exist");
            return; // Exit the method if the file doesn't exist
        }

        try (FileInputStream fileInputStream = new FileInputStream(file);
             FileChannel fileChannel = fileInputStream.getChannel()) {

            ByteBuffer fileContent = ByteBuffer.allocate(1024);
            int bytesRead;

            // Read the file content and send it to the client
            while ((bytesRead = fileChannel.read(fileContent)) > 0) {
                fileContent.flip(); // Prepare the buffer for writing
                while (fileContent.hasRemaining()) {
                    serveChannel.write(fileContent); // Write to the socket channel
                }
                fileContent.clear(); // Clear the buffer for the next read
            }

            // Check if we reached the end of the file
            if (bytesRead == -1) {
                System.out.println("End of file reached, upload completed.");
            } else {
                System.out.println("File upload completed, but not all data was read.");
            }
            serveChannel.shutdownOutput();


        } catch (IOException e) {
            System.err.println("Error during file upload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleClientRequests(SocketChannel serveChannel) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (true) {
                int bytesRead = serveChannel.read(buffer);

                if (bytesRead == -1) {
                    System.out.println("Client disconnected.");
                    break; // Exit the loop if the client disconnects
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
                        break;
                    case "DELETE":
                        deleteFile();
                        break;
                    case "RENAME":
                        renameFile();
                        break;
                    case "DOWNLOAD":
                        uploadFileToClient(serveChannel, clientRequestList[1]);
                        break;
                    case "UPLOAD":
                        downloadFileFromClient();
                        break;
                    default:
                        System.out.println("Invalid command, try again");
                }

                // Clear the buffer for the next read
                buffer.clear();
            }
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
