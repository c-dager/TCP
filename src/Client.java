import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws Exception {
        if(args.length != 2){
            System.out.println("Please provide <serverIP> and <serverPort>");
            return;
        }

        int serverPort = Integer.parseInt(args[1]);
        Scanner scanner = new Scanner(System.in);
        loop: while(true){
            System.out.println("Type the action you'd like to take:\nList\nDelete\nRename\nDownload\nUpload\nYou can also type Q to quit.\nAction: ");
            String action = scanner.nextLine().toUpperCase();
            SocketChannel channel = SocketChannel.open();
            channel.connect(new InetSocketAddress(args[0], serverPort));

            switch(action){
                case "LIST":
                    listFiles(channel);
                    break;
                case "DELETE":
                    deleteFile();
                    break;
                case "RENAME":
                    renameFiles();
                    break;
                case "DOWNLOAD":
                    downloadFile(channel, scanner);
                    break;
                case "UPLOAD":
                    uploadFile(channel, scanner);
                    break;
                case "Q":
                    quit(channel);
                    break loop;
                default:
                    System.out.println("Invalid command, try again");
            }
        }

    }

    private static void quit(SocketChannel channel) throws IOException {
        channel.close();
    }

    private static void uploadFile(SocketChannel channel, Scanner scanner) throws IOException {
        System.out.println("Enter the file name to be uploaded:");
        String filePath = scanner.nextLine();
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            System.err.println("File does not exist or is not a valid file.");
            return;
        }

        String fileName = file.getName();
        String request = "UPLOAD|" + fileName;

        // Send the request header
        ByteBuffer headerBuffer = ByteBuffer.wrap(request.getBytes());
        channel.write(headerBuffer);

        // Read the file contents
        byte[] fileContent = Files.readAllBytes(file.toPath());
        ByteBuffer contentBuffer = ByteBuffer.wrap(fileContent);

        // Send the file contents
        while (contentBuffer.hasRemaining()) {
            channel.write(contentBuffer);
        }

        System.out.println("File upload complete.");

    }

    private static void downloadFile(SocketChannel channel, Scanner scanner) throws IOException {
        System.out.println("Enter the file name to be downloaded");
        String fileName =  scanner.nextLine();
        String request = "DOWNLOAD|" + fileName;
        ByteBuffer byteBuffer = ByteBuffer.wrap(request.getBytes());
        channel.write(byteBuffer);
        //request to close channel from client to server direction
        FileOutputStream fileOutputStream = new FileOutputStream("ClientFiles/" + fileName, true);
        FileChannel fileChannel = fileOutputStream.getChannel();
        ByteBuffer fileContent = ByteBuffer.allocate(1024);

        while(channel.read(fileContent) >=0){
            fileContent.flip();
            fileChannel.write(fileContent);
            fileContent.clear();
        }
        fileOutputStream.close();
    }

    private static void renameFiles(SocketChannel channel, Scanner scanner) throws IOException {
        System.out.println("Enter the old file name (including path):");
        String oldFileName = scanner.nextLine();

        System.out.println("Enter the new file name (including path):");
        String newFileName = scanner.nextLine();

        // Construct the request header
        String request = "RENAME|" + oldFileName + "|" + newFileName;

            // Send the request header
            ByteBuffer headerBuffer = ByteBuffer.wrap(request.getBytes());
            channel.write(headerBuffer);

            System.out.println("Rename request sent");

        // Prepare to read the server's response
        ByteBuffer responseBuffer = ByteBuffer.allocate(2); // Assuming response is a single character
        int bytesRead = channel.read(responseBuffer);

        if (bytesRead > 0) {
            responseBuffer.flip(); // Prepare buffer for reading
            char response = (char) responseBuffer.get(); // Read the response character

            // Check the response from the server
            if (response == 'S') {
                System.out.println("Operation successful");
            } else if (response == 'F') {
                System.out.println("Operation Failed");
            } else {
                System.out.println("Unexpected response from server: " + response);
            }
        } else {
            System.out.println("No response received from server.");
        }

    }

    private static void deleteFile() {
    }

    private static void listFiles(SocketChannel channel) throws IOException {
        System.out.println("Requesting file list from server...");
        String request = "LIST";
        ByteBuffer byteBuffer = ByteBuffer.wrap(request.getBytes());
        channel.write(byteBuffer);

        // Read the response from the server
        ByteBuffer responseBuffer = ByteBuffer.allocate(1024);
        while (channel.read(responseBuffer) >= 0) {
            responseBuffer.flip();
            String response = new String(responseBuffer.array(), 0, responseBuffer.limit());
            if (response.startsWith("FILE_LIST:")) {
                // Extract the file list from the response
                String fileList = response.substring(11);
                System.out.println("Files on the server:");
                String[] files = fileList.split(",");
                for (String file : files) {
                    System.out.println(file.trim());
                }
                break;
            }
            else{
                System.out.println("No files found");
            }
            responseBuffer.clear();
        }
    }

}
