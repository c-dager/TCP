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
        Scanner scanner = new Scanner(System.in);
        System.out.println("Type the action you'd like to take: List, Delete, Rename, Download, or Upload: ");
        String action = scanner.nextLine().toUpperCase();
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(args[0], serverPort));

        switch(action){
            case "LIST":
                listFiles();
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
                uploadFile();
                break;
        }

        channel.close();


    }

    private static void uploadFile() {
    }

    private static void downloadFile(SocketChannel channel, Scanner scanner) throws IOException {
        System.out.println("Enter the file name to be downloaded");
        String fileName =  scanner.nextLine();
        String request = "DOWNLOAD " + fileName;
        ByteBuffer byteBuffer = ByteBuffer.wrap(fileName.getBytes());
        channel.write(byteBuffer);
        //request to close channel from client to server direction
        channel.shutdownOutput();
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

    private static void renameFiles() {
    }

    private static void deleteFile() {
    }

    private static void listFiles(){

    }

}
