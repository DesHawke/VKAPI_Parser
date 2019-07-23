import com.google.gson.*;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.users.Fields;

import java.util.*;


public class parser {
    private VkApiClient vk;


    public static void main(String[] args){
        List<Fields> userFieldList = Arrays.asList(
                Fields.SEX,
                Fields.CITY,
                Fields.COUNTRY,
                Fields.PHOTO_50,
                Fields.LAST_SEEN,
                Fields.UNIVERSITIES,
                Fields.SCHOOLS);

    TransportClient transportClient = HttpTransportClient.getInstance();
    VkApiClient vk = new VkApiClient(transportClient);

    int app_id=7058737;
    String access_token="779d5a5b2c5be69b6d134f734a2fbb51b087520d8aa948680eb021448801cc91ea172e19041c2d7be3e9c";

    UserActor APP= new UserActor(app_id,access_token);

    try {
        String first_people = vk.users().search(APP).fields(userFieldList).universityYear(2014).count(1).executeAsString();
        JsonObject converted=new JsonParser().parse(first_people).getAsJsonObject();
        JsonArray conv2= converted.getAsJsonObject("response").getAsJsonArray("items");

        /* Вывод пользователей полученных через users.search */
        /*
        if (conv2 != null) {
            int len = conv2.size();
            System.out.println(len);
            for (int i = 0; i < len; i++) {
                System.out.println(conv2.get(i).toString());
            }
        }
         */
        if (conv2 != null) {
            int len = conv2.size();
            //System.out.println(len);
            for (int i = 0; i < len; i++) {
                conv2.get(i).getAsJsonObject().remove("track_code");
                JsonElement test_id=conv2.get(i).getAsJsonObject().get("id");
                String friends = vk.friends().get(APP)
                    .userId(test_id.getAsInt())
                    .executeAsString();
                JsonObject friends_obj=new JsonParser().parse(friends).getAsJsonObject();
                JsonArray friends_arr= friends_obj.getAsJsonObject("response").getAsJsonArray("items");
                conv2.get(i).getAsJsonObject().add("friends",friends_arr);

                System.out.println(conv2.get(i).toString());
            }
        }

//    ClientResponse user_response = vk.users().get(APP)
//            .userIds(Integer.toString(user_id))
//            .fields(userFieldList)
//            .executeAsRaw();
//    ClientResponse friends_response = vk.friends().get(APP)
//            .userId(user_id)
//            .executeAsRaw();
//    System.out.println(user_response.getContent());
//    System.out.println(friends_response.getContent());
    } catch (ClientException e) {
        e.printStackTrace();
    }

}}
