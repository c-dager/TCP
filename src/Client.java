import java.io.FileOutputStream;
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
        String fileName = scanner.nextLine();

        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(args[0], serverPort));
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
        channel.close();


    }

}
