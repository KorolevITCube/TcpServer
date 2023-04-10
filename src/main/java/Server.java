import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

public class Server {

    private static final String counterRequest = "ff 21 35 56 1 bc 3 ";
    private static final String performanceRequest = "ff 21 35 56 2 bf 3 ";
    private static final String emulateZeroRequest = "ff 21 35 4b 56 f6 3 ";
    private static final String counterPerformanceRequest = "ff 21 35 56 10 fc be 3 ";

        public static void main(String[] args) throws InterruptedException {

//            byte[] array = new byte[6];
//            array[0] = (byte)0xff;
//            array[1] = (byte)0x22;
//            array[2] = (byte)0x20;
//            array[3] = (byte)0x49;
//            array[4] = countCRC(array);
//            array[5] = (byte)0xff;
//            String forLogg = "";
//            for (Byte a : array) {
//                forLogg += Integer.toHexString(Byte.toUnsignedInt(a)).toString() + " ";
//            }
            boolean flagBroken = true;
            int counter = 0;
            while(true) {
                try (ServerSocket server = new ServerSocket(9876)) {
                    Socket client = server.accept();
                    System.out.print("Connection accepted.");
                    DataOutputStream out = new DataOutputStream(client.getOutputStream());
                    System.out.println("DataOutputStream  created");
                    DataInputStream in = new DataInputStream(client.getInputStream());
                    System.out.println("DataInputStream created");
                    while (!client.isClosed()) {
                        System.out.println("Server reading from channel");
                        byte[] result = readResponse(in);
                        String forLog = "";
                        for (Byte a : result) {
                            forLog += Integer.toHexString(Byte.toUnsignedInt(a)).toString() + " ";
                        }
                        if (result[3] == 86 && result[4] == 01) {
                            System.out.println("Counter");
                            if (flagBroken) {
                                System.out.println("Waiting");
                                Thread.sleep(6000);
                                flagBroken = false;
                            }
                            byte[] response = new byte[11];
                            result = unEscapingReservedCharacters(result);
                            response[0] = result[0];
                            response[1] = result[2];
                            response[2] = result[1];
                            response[3] = result[3];
                            response[4] = result[4];
                            response[5] = 0x00;
                            response[6] = 0x00;
                            response[7] = 0x00;
                            response[8] = 0x00;
                            response[9] = countCRC(response,response.length-2);
                            if(counter < 10 || counter >15) {
                                response[10] = 0x03;
                            }else{
                                response[10] = 0x00;
                            }
                            counter++;
                            response = escapingResponse(response);
                            out.write(response);
                            out.flush();
                        } else if (result[3] == 86 && result[4] == 02) {
                            System.out.println("Performance");
                            byte[] response = new byte[11];
                            if (flagBroken) {
                                System.out.println("Waiting");
                                Thread.sleep(6000);
                                flagBroken = false;
                            }
                            result = unEscapingReservedCharacters(result);
                            response[0] = result[0];
                            response[1] = result[2];
                            response[2] = result[1];
                            response[3] = result[3];
                            response[4] = result[4];
                            response[5] = 0x00;
                            response[6] = 0x00;
                            response[7] = 0x00;
                            response[8] = 0x00;
                            response[9] = countCRC(response, response.length-2);
                            if(counter < 10 || counter >15) {
                                response[10] = 0x03;
                            }else{
                                response[10] = 0x00;
                            }
                            counter++;
                            response = escapingResponse(response);
                            out.write(response);
                            out.flush();
                        } else if (result[3] == 75 && result[4] == 86) {
                            System.out.println("EmulateZero");
                            if (flagBroken) {
                                System.out.println("Waiting");
                                Thread.sleep(6000);
                                flagBroken = false;
                            }
                            byte temp = result[1];
                            result[1] = result[2];
                            result[2] = temp;
                            out.write(result);
                            out.flush();
                        } else if (result[3] == 86 && result[4] == 16) {
                            System.out.println("Counter + Performance");
                            if (flagBroken) {
                                System.out.println("Waiting");
                                Thread.sleep(6000);
                                flagBroken = false;
                            }
                            byte[] sheet = new byte[5];
                            sheet[0] = 0x02;
                            sheet[1] = 0x01;
                            sheet[2] = 0x04;
                            sheet[3] = 0x05;
                            sheet[4] = 0x02;
                            byte[] response = new byte[15];
                            result = unEscapingReservedCharacters(result);
                            response[0] = result[0];
                            response[1] = result[2];
                            response[2] = result[1];
                            response[3] = result[3];
                            response[4] = result[4];
                            response[5] = 0x00;
                            response[6] = 0x00;
                            response[7] = 0x00;
                            response[8] = 0x00;
                            response[9] = 0x00;
                            response[10] = 0x00;
                            response[11] = 0x00;
                            response[12] = 0x00;
                            response[13] = countCRC(response, response.length-2);
                            if(counter >= 0) {
                                response[14] = 0x03;
                            }else{
                                response[14] = 0x00;
                            }
                            counter++;
                            response = escapingResponse(response);
                            //out.write(sheet);
                            out.write(response);
                            out.flush();
                        } else {
                            System.out.println("Dont understand");
                        }
                        Thread.sleep(100);
                    }
                    System.out.println("Client disconnected");
                    System.out.println("Closing connections & channels.");
                    in.close();
                    out.close();
                    client.close();
                    System.out.println("Closing connections & channels - DONE.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    private static byte countCRC(byte[] command,int length){
        byte cvc = 0b00000000;
        for (int i = 0; i < length; i ++) {
            cvc = (byte) (cvc ^ command[i]);
        }
        return cvc;
    }
    private static synchronized byte[] readResponse(InputStream is) throws IOException {
        try {
            var buffer = new byte[128];
            byte[] result;
            Arrays.fill(buffer, (byte) -1);
            var counter = 0;
            byte current = -1;
            do {
                current = (byte) is.read();
                buffer[counter] = current;
                counter++;
            } while (current != 0x03);
            result = Arrays.copyOfRange(buffer, 0, counter);
            String forLog = "";
            for(Byte a : result){
                forLog += Integer.toHexString(Byte.toUnsignedInt(a)).toString() + " ";
            }
            System.out.println("received array: " + forLog);
            return result;
        }catch(Exception e){
            System.out.println("Cant read data from input stream " + e.getCause());
            throw new IOException("Cant read data from input stream",e.getCause());
        }
    }

    private static ArrayList<Byte> escapingReservedCharacters(ArrayList<Byte> command) {
        for (var i = 1; i < command.size()-1; i++){
            if(command.get(i) == (byte) 0xff){
                command.set(i, (byte) 0x00);
                command.add(i,(byte) 0x10);
            }else if(command.get(i) == (byte) 0x03){
                command.set(i, (byte) 0xfc);
                command.add(i,(byte) 0x10);
            }else if(command.get(i) == (byte) 0x10){
                command.set(i, (byte) 0xef);
                command.add(i,(byte) 0x10);
            }
        }
        return command;
    }

    private static byte[] escapingResponse(byte[] response){
        var command = new ArrayList<Byte>();
        for (byte b:response) {
            command.add(b);
        }
        var resCommand = escapingReservedCharacters(command);
        var request = new byte[resCommand.size()];
        IntStream.range(0, command.size()).forEach(i -> request[i] = resCommand.get(i));
        return request;
    }

    private static byte[] unEscapingReservedCharacters(byte[] response){
        var responseList = new ArrayList<Byte>();
        for (var i = 0; i < response.length;i++){
            if(response[i] == 0x10){
                if(response[i+1] == (byte) 0x00){
                    responseList.add((byte)0xff);
                }else if(response[i+1] == (byte) 0xfc){
                    responseList.add((byte)0x03);
                }else if(response[i+1] == (byte) 0xef){
                    responseList.add((byte)0x10);
                }
                i++;
            }else{
                responseList.add(response[i]);
            }
        }
        var result = new byte[responseList.size()];
        IntStream.range(0, responseList.size()).forEach(i -> result[i] = responseList.get(i));
        return result;
    }
}
