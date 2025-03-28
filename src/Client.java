
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws Exception {
        if(args.length != 2){
            System.out.println("Please provide <serverIP> and <serverPort>");
            return;
        }

        int serverPort = Integer.parseInt(args[1]);
        String serverIP = args[0];
        Scanner scanner = new Scanner(System.in);
        loop: while(true){
            //SocketChannel channel = SocketChannel.open();
            //channel.connect(new InetSocketAddress(serverIP, serverPort));
            System.out.println("Type the action you'd like to take:\n>List\n>Delete\n>Rename\n>Download\n>Upload\nYou can also type Q to quit.\nAction: ");
            String action = scanner.nextLine().toUpperCase();

            switch(action){
                case "LIST":
                    listFiles(serverIP, serverPort);
                    break;
                case "DELETE":
                    deleteFile(serverIP, serverPort, scanner);
                    break;
                case "RENAME":
                    renameFile(serverIP, serverPort, scanner);
                    break;
                case "DOWNLOAD":
                    //es.submit(new downloadTask(channel, scanner));
                    downloadFile(serverIP, serverPort, scanner);
                    break;
                case "UPLOAD":
                    //es.submit(new uploadTask(channel, scanner));
                    uploadFile(serverIP, serverPort, scanner);
                    break;
                case "Q":
                    quit();
                    break loop;
                default:
                    System.out.println("Invalid command, try again");
            }
            //channel.close();
        }
    }

    private static void quit() throws IOException {
        System.out.println("Bye!");
    }

    private static void uploadFile(String serverIP, int serverPort, Scanner scanner) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(serverIP, serverPort));
        System.out.println("Enter the file name to be uploaded:");
        String filePath = scanner.nextLine();
        File file = new File("ClientFiles", filePath);

        if (!file.exists() || !file.isFile()) {
            System.err.println("File does not exist or is not a valid file.");
            return;
        }

        String fileName = file.getName();
        String request = "UPLOAD|" + fileName;

        // Send the request header
        ByteBuffer headerBuffer = ByteBuffer.wrap(request.getBytes());
        channel.write(headerBuffer);

        // Send the file contents
        try (FileInputStream fileInputStream = new FileInputStream(file);
             FileChannel fileChannel = fileInputStream.getChannel()) {
            Thread.sleep(5000);
            ByteBuffer fileBuffer = ByteBuffer.allocate(1024);
            // Read the file content and send it to the client
            while (fileChannel.read(fileBuffer) > 0){
                fileBuffer.flip();
                channel.write(fileBuffer);
                fileBuffer.clear();
            }
            channel.shutdownOutput();

        } catch (IOException e) {
            System.err.println("Error during file upload: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Prepare to read the server's response
        ByteBuffer responseBuffer = ByteBuffer.allocate(10); // Assuming response is a single character
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
        channel.close();


    }

    private static void downloadFile(String serverIP, int serverPort, Scanner scanner) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(serverIP, serverPort));
        System.out.println("Enter the file name to be downloaded:");
        String fileName = scanner.nextLine();
        String request = "DOWNLOAD|" + fileName;
        ByteBuffer requestBuffer = ByteBuffer.wrap(request.getBytes());
        channel.write(requestBuffer);
        channel.shutdownOutput();
        // Create the output file stream and channel
        try (FileOutputStream fileOutputStream = new FileOutputStream("ClientFiles/" + fileName);
             FileChannel fileChannel = fileOutputStream.getChannel()) {

            ByteBuffer fileContent = ByteBuffer.allocate(1024);
            int bytesRead;

            System.out.println("Downloading file...");

            // Read the file content from the channel
            while (true) {
                bytesRead = channel.read(fileContent);
                if (bytesRead == -1) {
                    System.out.println("End of stream reached, download completed.");
                    break; // Exit the loop if the end of the stream is reached
                } else if (bytesRead > 0) {
                    fileContent.flip(); // Prepare the buffer for writing
                    while (fileContent.hasRemaining()) {
                        fileChannel.write(fileContent); // Write to the file channel
                    }
                    fileContent.clear(); // Clear the buffer for the next read
                }
            }
            channel.close();


        } catch (IOException e) {
            System.err.println("Error during file download: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void renameFile(String serverIP, int serverPort, Scanner scanner) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(serverIP, serverPort));
        System.out.println("Enter the old file name:");
        String oldFileName = scanner.nextLine();

        System.out.println("Enter the new file name:");
        String newFileName = scanner.nextLine();

        // Construct the request header
        String request = "RENAME|" + oldFileName + "|" + newFileName;

            // Send the request header
            ByteBuffer headerBuffer = ByteBuffer.wrap(request.getBytes());
            channel.write(headerBuffer);
            channel.shutdownOutput();

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
        channel.close();

    }

    private static void deleteFile(String serverIP, int serverPort, Scanner scanner) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(serverIP, serverPort));
        System.out.println("Enter the file name to be deleted:");
        String fileName = scanner.nextLine();

        // Construct the request header
        String request = "DELETE|" + fileName;
        // Send the request header
        ByteBuffer headerBuffer = ByteBuffer.wrap(request.getBytes());
        channel.write(headerBuffer);
        channel.shutdownOutput();

        System.out.println("Delete request sent: " + request);

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
        channel.close();
    }

    private static void listFiles(String serverIP, int serverPort) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(serverIP, serverPort));
        System.out.println("Requesting file list from server...");
        String request = "LIST";
        ByteBuffer byteBuffer = ByteBuffer.wrap(request.getBytes());
        channel.write(byteBuffer);
        channel.shutdownOutput();
        // Read the response from the server
        ByteBuffer responseBuffer = ByteBuffer.allocate(1024);
        while (channel.read(responseBuffer) >= 0) {
            responseBuffer.flip();
            String response = new String(responseBuffer.array(), 0, responseBuffer.limit());
            if (response.startsWith("FILE_LIST:")) {
                // Extract the file list from the response
                String fileList = response.substring(10);
                System.out.println("Files on the server:");
                String[] files = fileList.split(",");
                for (String file : files) {
                    System.out.println(file.trim());
                }
                System.out.println();
                break;
            }
            else{
                System.out.println("No files found");
            }
            responseBuffer.clear();
            channel.close();
        }
    }

}
