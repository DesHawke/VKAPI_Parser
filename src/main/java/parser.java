import com.vk.api.sdk.client.ClientResponse;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;


public class parser {
    private static TransportClient transportClient = HttpTransportClient.getInstance();
    private static VkApiClient vk = new VkApiClient(transportClient);

    private static int app_id = 7058737;
    private static String access_token = "779d5a5b2c5be69b6d134f734a2fbb51b087520d8aa948680eb021448801cc91ea172e19041c2d7be3e9c";
    //private static String access_token = "6a99b6dce7c59daac6a76e3703ab3b75b6ddc9f4ee569c9ef1173c07cee4638d2c47e9eab2c22753863af";
    private static UserActor APP = new UserActor(app_id, access_token);

    /*
        Код алгоритма в VKScript-формате для выполнения запроса execute
        Принимает в качестве параметров строку из id пользователей, перечисленных через запятую и их количество
     */
    private static String getScript(String str, int count){
        return "var user_ids="+str+";\n" +
                "var count="+count+";\n" +
                "var all_users= API.users.get({\"user_ids\": user_ids, \"fields\": \"sex, city, country, photo_50, last_seen, universities, school\"});\n" +
                "var result=[];\n" +
                "var id=0;\n" +
                "while(id<count){\n" +
                "    if(all_users[id].is_deactivated==null){\n" +
                "        if(all_users[id].is_closed==false){\n" +
                "            var friends=API.friends.get({user_id: user_ids[id]});\n" +
                "            if(friends.count>0 && friends.count<1000){\n" +
                "                var result_user={id:all_users[id].id,\n" +
                "                                first_name:all_users[id].first_name,\n" +
                "                                last_name:all_users[id].last_name,\n" +
                "                                sex:all_users[id].sex,\n" +
                "                                city:all_users[id].city,\n" +
                "                                country:all_users[id].country,\n" +
                "                                photo_50:all_users[id].photo_50,\n" +
                "                                last_seen:all_users[id].last_seen,\n" +
                "                                universities:all_users[id].universities,\n" +
                "                                schools:all_users[id].schools,\n" +
                "                                friends:friends.items};\n" +
                "                result.push(result_user);}}}\n" +
                "    id=id+1;}\n" +
                "return result;";
    }

    private static JSONArray getMoreUsers(JSONArray users) throws InterruptedException, ClientException {
        int len=users.length();
        int div=len/24;
        int mod=len%24;

        JSONArray result=new JSONArray();

        for (int i=0; i<div;i++){
            int count=24*i;
            JSONArray ids= new JSONArray();

            for(int j=0;j<24;j++){
                ids.put(users.get(j+count));
            }

            // Задержка из-за ограничения VK на 3 запроса в секунду
            Thread.sleep(350);
            ClientResponse resp=vk.execute().code(APP,getScript(ids.toString(),ids.length())).executeAsRaw();

            JSONObject r = new JSONObject(resp.getContent());
            if (!r.isNull("response")) {
                JSONArray res= r.getJSONArray("response");
                for(int l=0;l<res.length();l++){
                    result.put(res.getJSONObject(l));
                }
            }
        }

        JSONArray ids_last= new JSONArray();
        for (int k=len-mod;k<len;k++){
            ids_last.put(users.get(k));
        }
        Thread.sleep(350);
        ClientResponse resp=vk.execute().code(APP,getScript(ids_last.toString(),ids_last.length())).executeAsRaw();

        JSONObject r = new JSONObject(resp.getContent());
        if (!r.isNull("response")) {
            JSONArray res= r.getJSONArray("response");
            for(int l=0;l<res.length();l++){
                result.put(res.getJSONObject(l));
            }
        }
        return result;
    }

    /*
        Функция извлечения информации об одном пользователе
     */
    private static JSONObject getUser(int user_id) throws ClientException, InterruptedException {
        String code=
                "var user_id="+user_id+
                "var info= API.users.get({\"user_ids\": user_id,\"fields\": \"sex, city, country, photo_50,last_seen,universities,school\"});" +
                "var is_deactivated=info@.is_deactivated;"+
                "if(is_deactivated!=null){"+
                    "var is_closed=info@.is_closed;"+
                    "if(is_closed){"+
                        "var friends=API.friends.get({\"user_id\": user_id});"+
                        "if(friends.count>0 && friends.count<1000){"+
                            "var result={id:info[0].id,"+
                                        "first_name:info[0].first_name,"+
                                        "last_name:info[0].last_name,"+
                                        "sex:info[0].sex,"+
                                        "city:info[0].city,"+
                                        "country:info[0].country,"+
                                        "photo_50:info[0].photo_50,"+
                                        "last_seen:info[0].last_seen,"+
                                        "universities:info[0].universities,"+
                                        "schools:info[0].schools,"+
                                        "friends:friends.items };"+
                            "return result;}}}" +
                        "else return null;";

        Thread.sleep(350);
        ClientResponse resp=vk.execute().code(APP,code).executeAsRaw();

        JSONObject user = new JSONObject(resp.getContent());
        if (!user.isNull("response")) {
            return user.getJSONObject("response");
        }
        else return null;
    }

    /*
        Функция поиска пользователей. Поддерживает поиск по городу, университету, школе
     */
    private static JSONArray search(int number, String param, String value) throws ClientException {
        ClientResponse search_resp = null;
        Scanner in = new Scanner(System.in);
        ClientResponse response;
        String code;
        switch (param) {
            case ("city"):
                // Наиболее вероятный город
                response = vk.database().getCities(APP, 1).q(value).executeAsRaw();
                JSONObject City_parse = new JSONObject(response.getContent());
                JSONObject arr = City_parse.getJSONObject("response").getJSONArray("items").getJSONObject(0);

                int city_id = arr.getInt("id");
                code="var result= API.users.search({\"city\":"+city_id+",\"count\":"+number+"});\n" +
                        "return result.items@.id;";
                search_resp=vk.execute().code(APP,code).executeAsRaw();
                break;

            case ("university"):
                response = vk.database().getUniversities(APP).q(value).count(5).executeAsRaw();
                JSONObject uni_parse = new JSONObject(response.getContent());
                JSONArray array = uni_parse.getJSONObject("response").getJSONArray("items");

                System.out.println("Выберите нужный университет по ID:");

                for (int i=0;i<array.length();i++) {
                    System.out.println("ID " + array.getJSONObject(i).get("id") + "\t" +array.getJSONObject(i).get("title"));
                }
                int uni_id = in.nextInt();
                code="var result= API.users.search({\"university\":"+uni_id+",\"count\":"+number+"});\n" +
                        "return result.items@.id;";
                search_resp=vk.execute().code(APP,code).executeAsRaw();
                break;

            case ("school"):
                System.out.println("Выберите город:");
                String name=in.nextLine();
                ClientResponse city_response = vk.database().getCities(APP, 1).q(name).executeAsRaw();
                JSONObject city_school = new JSONObject(city_response.getContent());
                JSONObject city_school_arr = city_school.getJSONObject("response").getJSONArray("items").getJSONObject(0);

                int city_school_id = city_school_arr.getInt("id");
                response = vk.database().getSchools(APP, city_school_id).q(value).count(5).executeAsRaw();
                JSONObject school_parse = new JSONObject(response.getContent());
                JSONArray school_array = school_parse.getJSONObject("response").getJSONArray("items");

                System.out.println("Выберите нужную школу по ID:");
                for (int i=0;i<school_array.length();i++) {
                    System.out.println("ID " + school_array.getJSONObject(i).get("id") + "\t" +school_array.getJSONObject(i).get("title"));
                }
                int school_id = in.nextInt();
                code="var result= API.users.search({\"school\":"+school_id+",\"count\":"+number+"});\n" +
                        "return result.items@.id;";
                search_resp=vk.execute().code(APP,code).executeAsRaw();
                break;
            default:
                break;
        }

        assert search_resp != null;
        JSONObject obj = new JSONObject(search_resp.getContent());
        return obj.getJSONArray("response");
    }

    public static void main(String[] args) throws InterruptedException, ClientException {

        JSONArray result = new JSONArray();

        //Входные данные
        String param;
        String value;

        // Количество людей из поиска (максимум 1000)
        int number=20;

        // Глубина поиска
        int deep=2;

        // ! ПРОБЛЕМЫ С РУССКОЙ КОДИРОВКОЙ !
        Scanner in = new Scanner(System.in);
        System.out.println("Input parameter: ");
        param = in.nextLine();
        System.out.println("Input value: ");
        value = in.nextLine();

        // Первоначальный поиск
        JSONArray searching_people = search(number, param, value);

        JSONArray[] iter=new JSONArray[deep];

        // Отфильтрованные пользователи - первая итерация
        iter[0]=getMoreUsers(searching_people);

        // Проход по друзьям (друзья друзей)
        for (int P=1;P<deep;P++) {

            // Проход по друзьям пользователей предыдущей итерации
            iter[P]=new JSONArray();
            for (int i = 0; i < iter[P-1].length(); i++) {
                JSONArray friends_list = iter[P-1].getJSONObject(i).getJSONArray("friends");
                JSONArray temp=getMoreUsers(friends_list);
                for (int t = 0; t < temp.length(); t++) {
                    iter[P].put(temp.getJSONObject(t));
                }
            }
        }

        // добавление пользователей всех итераций в конечный результат
        for (int P=0;P<deep;P++) {
            for(int i=0;i<iter[P].length();i++){
                result.put(iter[P].getJSONObject(i));
            }
        }

        System.out.println(result.length());

    }
}

