import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.conf.Configuration;
//import org.apache.hadoop.fs.RemoteIterator;

import org.testng.Assert;
import org.testng.annotations.*;


/* Быстрый запуск из InteliJ IDEA:
 * Select Run/Debug Configuation -> выбрать конфигурацию "Maven",
 *   -> указать "Working Directory",
 *  -> указать команду для запуска jar-файла "Command Line": package install
 *  -> запустить тесты с этой конфигурацией
 */
public class TestHdfsClient {

    static Configuration hdfs_config = null;
    static FileSystem hdfs = null;
    static FileStatus [] hdfs_file_status = null;
    static Path[] paths = null;
    private static final String CLIENT = "hdfs://localhost:54310"; // if hdfs in local mode

    /*
     * Выпоняется один раз перед запуском тестов в классе
     */
    @BeforeClass
    public void before_class_testing() throws Exception{
        hdfs_config = new Configuration();
        hdfs = FileSystem.get(new URI(CLIENT), hdfs_config);
        System.out.println("==== Open hdfs-connection =====");
    }


    /*
     *Проверить подключение к файловой системе Hadoop HDFS
     * return: Current file system is HDFS - ОК
     *         (DFS[DFSClient[clientName=DFSClient_NONMAPREDUCE_777094943_1, ugi=larisa (auth:SIMPLE)]],
     *         class org.apache.hadoop.hdfs.DistributedFileSystem)
     */
    @Test
    public void test_check_hdfs_connection() throws Exception {
        if (hdfs instanceof DistributedFileSystem) {
            System.out.println("\n === Current file system is HDFS - ОК ("+hdfs+ ","+hdfs.getClass()+")==== \n");
        } else {
            Assert.fail("\n ==== Other type of file system: " + hdfs + "\n" + hdfs.getClass() + "=== \n");
        }
    }


    /*
     * Обновление конфигурационного файла HDFS, изменение коэффициента репликации
     * return: dfs.namenode.replication.min i: 1
     *         dfs.replication.max i: 512
     *         dfs.replication i: 4
     *         ....
     */
    @Test(dependsOnMethods={"test_check_hdfs_connection"})
    public void test_update_config() throws Exception {
        hdfs_config.set("dfs.replication", "4");
        Iterator<Entry<String, String>> iter = hdfs_config.iterator();
        while (iter.hasNext()) {
            Entry<String, String> entry = iter.next();
            System.out.println(entry.getKey() + " i: " + entry.getValue());
        }
        String new_replic = hdfs_config.get("dfs.replication");
        Assert.assertEquals(new_replic, "4");
        System.out.println("====== New dfs.replication is: " + new_replic + " - OK ====== \n");
    }


    /*
     * Прочитать список файлов hdfs
     */
    @Test(dependsOnMethods={"test_check_hdfs_connection"})
    public void test_file_list() throws Exception, IOException {
        try {
           hdfs_file_status = hdfs.listStatus(new Path(CLIENT+"/")); // +"/user/hadoop"));
           Path file_path = new Path(CLIENT+"/");
           paths = FileUtil.stat2Paths(hdfs_file_status);
           System.out.println("==== List of directory files =====");
           for(Path path : paths) {
               System.out.println(path);
           }
           } catch (IOException e) {
                Assert.fail("=========== " +e.getMessage() + "=========== ");
           }
    }

    /*
     * Выпоняется один раз после прохождения всех тестов в классе
    */
    @AfterClass
    public void after_class_testing() throws Exception{
        hdfs.close();
        System.out.println("==== Close hdfs-connection =====");
    }

}

