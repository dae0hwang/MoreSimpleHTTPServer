import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.Socket;

public class RequestHandler implements Runnable {
    private Socket connection;
    private static ObjectMapper objectMapper = new ObjectMapper();
    private ResponseService responseService = new ResponseService();

    public RequestHandler(Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        try (InputStream inputStream = connection.getInputStream();
             OutputStream outputStream = connection.getOutputStream()) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

            /*
            Request message의 Request Line을 requesteMethod 객체에 저장한다.
            ex) Request Line -> GET /doc/test.html HTTP/1.1
             */
            RequestMethod requestMethod = responseService.readFirstLIneAndImplementRequestMethod(bufferedReader);
            System.out.println(requestMethod.toString());

            /*
            Request Line 내용(GET, POST,등)을 기준과 switch case문을 사용하여,
            서버가 해야할 동작들을 처리한다.
             */
            switch (requestMethod.getMethod()) {
                case "GET":
                    switch (requestMethod.getRequestType()) {
                        case "time":
                            byte[] timeJsonBytes = responseService.responseFromGETAndTime();
                            responseService.sendResponseFromGETAndTime(dataOutputStream, timeJsonBytes);
                            break;
                        case "text":
                            String storageString = responseService.responseFromGETAndText(requestMethod);
                            responseService.sendResponseFromGETAndText(dataOutputStream, requestMethod, storageString);
                            break;
                    }
                    break;
                case "POST":
                    switch (requestMethod.getRequestType()) {
                        case "text":
                            String messageBody = responseService.saveDateFromPostAndText(requestMethod, bufferedReader);
                            TreatStateCode treatStateCode =
                                responseService.treatFromPostAndText(messageBody, requestMethod);
                            responseService.responseFromPostAndText(treatStateCode, dataOutputStream);
                            break;
                    }
                    break;
            }
            /*
            클라이언트가 보낸 textId 별로, String 정보를 저장한 stringStorage를 서버 쪽에서 확인한다.
             */
            System.out.println("checking StringStorage: " + responseService.stringStorage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
