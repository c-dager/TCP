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
        ServerSocketChannel listenChannel = ServerSocketChannel.open();
        listenChannel.bind(new InetSocketAddress(3000));
        while(true){
            SocketChannel serveChannel = listenChannel.accept();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = serveChannel.read(buffer);
            buffer.flip();
            byte[] a = new byte[bytesRead];
            buffer.get(a);
            String clientRequest = new String(a);
            String[] clientRequestList = clientRequest.split("\\|");
            String action = clientRequestList[0];

            switch(action){
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
            //serveChannel.close();
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


    private static void uploadFileToClient(SocketChannel serveChannel, String fileName) throws IOException {
        System.out.println("File name message: " + fileName);

        File file = new File("ServerFiles/" + fileName);
        if(!file.exists()){
            System.out.println("File doesn't exist");
        }
        else{
            FileInputStream fileInputStream = new FileInputStream(file);
            FileChannel fileChannel = fileInputStream.getChannel();
            ByteBuffer fileContent = ByteBuffer.allocate(1024);

            int byteRead = 0;
            do {
                byteRead = fileChannel.read(fileContent);
                fileContent.flip();
                serveChannel.write(fileContent);
                fileContent.clear();
            }while (byteRead >=0);

            fileInputStream.close();
            serveChannel.close();

        }
    }
}
