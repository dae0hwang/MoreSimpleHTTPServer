import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResponseService {
    private ObjectMapper objectMapper = new ObjectMapper();
    RequestTool requestTool = new RequestTool();
    protected static ConcurrentHashMap<String, String> stringStorage = new ConcurrentHashMap<>();

    public  RequestMethod readFirstLIneAndImplementRequestMethod(BufferedReader bufferedReader)
        throws IOException {
        RequestMethod requestMethod = new RequestMethod();
        String firstLine = bufferedReader.readLine();
        String[] firstLineArray = firstLine.split(" ");
        requestMethod.setMethod(firstLineArray[0]);
        String requestTypeAndTextId = firstLineArray[1].substring(1, firstLineArray[1].length());
        if (requestTypeAndTextId.contains("/")) {
            String[] requestTypeAndTextIdArray = requestTypeAndTextId.split("/");
            requestMethod.setRequestType(requestTypeAndTextIdArray[0]);
            requestMethod.setTextId(requestTypeAndTextIdArray[1]);
        } else {
            requestMethod.setRequestType(requestTypeAndTextId);
        }
        return requestMethod;
    }

    public byte[] responseFromGETAndTime() throws JsonProcessingException {
        TimeResponseMessageBody timeResponseMessageBody = new TimeResponseMessageBody();
        timeResponseMessageBody.setTime(String.valueOf(new Date()));
        byte[] timeJsonBytes = objectMapper.writeValueAsBytes(timeResponseMessageBody);
        return timeJsonBytes;
    }

    public void sendResponseFromGETAndTime(DataOutputStream dataOutputStream, byte[] timeJsonBytes)
        throws IOException {
        dataOutputStream.writeBytes(ResponseStateCode.response200OK.getStateMessage());
        dataOutputStream.write(timeJsonBytes, 0, timeJsonBytes.length);
        dataOutputStream.flush();
    }

    public String responseFromGETAndText(RequestMethod requestMethod){
        String textId = requestMethod.getTextId();
        if (textId != null) {
            String storageString = stringStorage.get(textId);
            if (storageString != null) {
                return storageString;
            }
        }
        return null;
    }

    public void sendResponseFromGETAndText(DataOutputStream dataOutputStream, RequestMethod requestMethod,
                                           String storageString) throws IOException {
        String textId = requestMethod.getTextId();
        if (textId == null) {
            dataOutputStream.writeBytes(ResponseStateCode.response404NotFound.getStateMessage());
        } else {
            if (storageString == null) {
                dataOutputStream.writeBytes(ResponseStateCode.response404NotFound.getStateMessage());
            } else {
                dataOutputStream.writeBytes(ResponseStateCode.response200OK.getStateMessage());
                dataOutputStream.writeBytes(storageString);
                dataOutputStream.flush();
            }
        }
    }

    public String saveDateFromPostAndText
        (RequestMethod requestMethod, BufferedReader bufferedReader) throws IOException {
        String textId = requestMethod.getTextId();
        if (textId == null) {
            return null;
        } else {
            Map<String, String> headerInformation = requestTool.readHeader(bufferedReader);
            if (headerInformation.containsKey("Content-Length")) {
                int messageBodyLength = Integer.parseInt(headerInformation.get("Content-Length").trim());
                String messageBody = requestTool.readDate(bufferedReader, messageBodyLength);
                return messageBody;
            } else {
                return null;
            }
        }
    }

    public TreatStateCode treatFromPostAndText(String messageBody, RequestMethod requestMethod) {
        if (messageBody == null) {
            return TreatStateCode.FAIL;
        } else {
            String textId = requestMethod.getTextId();
            stringStorage.put(textId, messageBody);
            return TreatStateCode.SUCCESS;
        }
    }

    public void responseFromPostAndText(TreatStateCode treatStateCode, DataOutputStream dataOutputStream)
        throws IOException {
        if (treatStateCode == TreatStateCode.SUCCESS) {
            dataOutputStream.writeBytes(ResponseStateCode.response201Created.getStateMessage());
        } else if (treatStateCode == TreatStateCode.FAIL) {
            dataOutputStream.writeBytes(ResponseStateCode.response404NotFound.getStateMessage());
        }
    }
}
