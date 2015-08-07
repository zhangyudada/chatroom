package chatroom;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/**
 *
 * @author zhangyu
 */
public class ChatroomClient {
    
    Selector selector                           = null;                         //选择器，全局的
    SocketChannel socketChannel                 = null;                         //服务器端通道
    private final int port                      = 12345;                        //服务器端的端口
    private final Charset charset               = Charset.forName("UTF-8");     //统一采用UTF-8(Unicode)编码和解码，避免出现乱码
    //由数组实现的List，允许对元素进行快速随机访问，这里存放String类型的本地历史消息
    private ArrayList historyArrayList          = new ArrayList();
    
    public void initClient() throws IOException{
        //尝试与服务器建立连接，若服务器没有开启，则抛出java.net.ConnectException异常，需进行异常处理
        try{
            selector = Selector.open();                                         //新建选择器对象，控制各种channel
            //根据服务器的套接字连接服务器，127.0.0.1为环回测试地址，port是服务器端口
            socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1",port));

            socketChannel.configureBlocking(false);                             //与Selector共同作用时，必须设为非阻塞监听模式       
            //将socketChannel注册到selector，注册事件为监听读请求
            //返回一个SelectionKey类的对象（这里没有必要使用这个对象，故没有对象名）
            //这个对象联系着socketChannel和selector
            socketChannel.register(selector, SelectionKey.OP_READ);
            //开启一个新线程，获取服务器发送过来的消息
            new Thread(new clientRunnable()).start();  
            //获取控制台的输入消息
            scanConsole(socketChannel); 
        }catch(ConnectException e){
            System.out.println("服务器未开启，3秒后自动关闭客户端");
            //当前线程暂停3秒后关闭，
            try{
                Thread thread = Thread.currentThread();
                thread.sleep(3000);//暂停3秒后程序继续执行
                System.exit(0);
            }catch (InterruptedException  ex) {}
        }
               
    }
    
    //从控制台扫描，即获取控制台输入的消息，并作相关处理
    void scanConsole(SocketChannel socketChannel) throws IOException{
        //创建一个Scanner，控制台会一直等待输入，直到敲回车键结束，把所输入的内容传给Scanner，作为扫描对象
        Scanner scanner = new Scanner(System.in);
        //如果在此扫描器的输入中存在另一行，则返回true       
        while(scanner.hasNextLine()){
            //查找并返回来自此扫描器的下一个完整标记
            String msg = scanner.nextLine();
            
            //不允许发空消息（即控制台什么也没输入直接就回车了）
            if(msg.length() == 0)
                continue;
            //消息不为空（但消息可能是空格、制表符等不能显示成字符的符号）
            else{
                //试图将msg以空格分隔再存到数组msgArray中
                //如果控制台输入的是若干个空格，则msgArray中什么都不会存
                //这时若引用msgArray[0]就会抛出数组下标越界异常ArrayIndexOutOfBoundsException
                try{
                    //将msg以空格分隔再存到数组msgArray中
                    String[] msgArray = msg.split(" ");
                    switch(msgArray[0]){
                        //查询本地历史消息记录，不用向服务器发消息，不需要进行异常处理
                        case "/history": history(msgArray);break;
                        //客户端要求退出，则先把消息发给服务器（服务器再就知道客户端想退出了，然后广播客户端退出消息）
                        //就算"/quit"后面跟了其他字符串，都无视之，只把"/quit"发给服务器
                        //接着客户端关闭程序
                        case "/quit"   :
                            //如果服务器关闭了，则向服务器发送消息会抛出java.io.IOException异常
                            //需要进行异常处理
                            try{
                                socketChannel.write(charset.encode("/quit"));
                                System.exit(0);
                                break;
                            }catch(IOException e){
                                System.out.println("服务器已经关闭，3秒后自动关闭客户端");
                                //当前线程暂停3秒后关闭，
                                try{
                                    Thread thread = Thread.currentThread();
                                    thread.sleep(3000);//暂停3秒后程序继续执行
                                    System.exit(0);
                                }catch (InterruptedException  ex) {}
                            }
                        
                        //其他情况则直接将消息发送到服务器，由服务器处理
                        default        : 
                            //如果服务器关闭了，则向服务器发送消息会抛出java.io.IOException异常
                            //需要进行异常处理
                            try{
                                socketChannel.write(charset.encode(msg));
                            }catch(IOException e){
                                System.out.println("服务器已经关闭，3秒后自动关闭客户端");
                                //当前线程暂停3秒后关闭，
                                try{
                                    Thread thread = Thread.currentThread();
                                    thread.sleep(3000);//暂停3秒后程序继续执行
                                    System.exit(0);
                                }catch (InterruptedException  ex) {}
                            }
                    }
                }catch(ArrayIndexOutOfBoundsException e){
                    System.out.println("请不要只输入空格！");   
                }
            }  
        }
    }
    
    //查询本地历史消息记录
    void history(String[] msgArray){
        //把historyArrayList中的所有元素转化为字符串数组
        //String[] historyArray = (String[])historyArrayList.toArray();         //这种转换是错的
        Object[] array = historyArrayList.toArray();
        //获得数组长度（即记录的消息数）
        int size = array.length;
        String[] historyArray = new String[size];
        for(int i = 0;i < size;i++)
            historyArray[i] = (String)array[i];

        //msgArray长度为1，即为不带参数的"/history"命令，默认显示最近50条消息（没有50条就显示全部消息）
        if(msgArray.length == 1){
            //没有消息
            if(size == 0)
                System.out.println("当前没有历史消息记录");
            //没有50条就显示全部消息
            else if(size <= 50 && size > 0){
                for(int i = 0;i < size;i++)
                    System.out.println("#" + (i + 1) + " " + historyArray[i]);
            }
            //消息数大于50条，则只显示最近的50条消息
            else{
                for(int i = size - 50;i < size;i++)
                    System.out.println("#" + (i + 51 - size) + " " + historyArray[i]); 
            }
        }

        //msgArray长度等于3，即为合法的带参数的"/history"命令
        else if(msgArray.length == 3){
            //尝试将第2和第3个参数转换为整数，转换失败则抛出NumberFormatException异常
            try{
                int start_index = Integer.parseInt(msgArray[1]);
                int max_count   = Integer.parseInt(msgArray[2]);
                //判读数字是不是正整数
                if(start_index < 1 || max_count < 1)
                    System.out.println("/history后面的两个参数必须为正整数！");
                //数字是正整数
                else{
                    //查看的最大位置比现有消息记录还长，则返回查询不到的提示
                    if(start_index > size)
                        System.out.println("现有消息只有"+size+"条，请重新选择start_index");
                    //查看的最大位置在现有消息记录中
                    else{
                        //要查询的起始位置超过消息记录的起始位置的，就从消息记录的起始位置查询
                        if((start_index - max_count) < 0)
                            for(int i = 0;i < start_index;i++)
                                System.out.println("#" + (i + 1) + " " + historyArray[i]);
                        //要查询的起始位置在消息记录的中，就从要查询的起始位置开始查询
                        else
                            for(int i = start_index - max_count;i < start_index;i++)
                                System.out.println("#" + (i + max_count - start_index + 1) + " " + historyArray[i]); 
                    }  
                }
            }catch(NumberFormatException e){
                System.out.println("/history后面的两个参数必须为正整数！");
            }   
        }
        
        //其他长度的msgArray都是不合法的命令，报错
        else
            System.out.println("命令不合法，合法的命令格式为：/history [start_index max_count]");    
    }
    
    //私有的内部类，只能再本ChatroomClient类中使用
    //通过实现Runable接口来实现多线程设计
    private class clientRunnable implements Runnable{
        //线程体，实现Runnable接口
        public void run(){
            try{
                while(true){
                    int readyChannels = selector.select();                      //返回值为就绪的通道数
                    if(readyChannels == 0) continue;                            //没有通道就绪就接着阻塞
                    Set selectedKeys = selector.selectedKeys();                 //将就绪通道的键放到一个Set（集）对象中
                    Iterator iterator = selectedKeys.iterator();                //Set类的方法iterator()返回一个Iterator对象
                                                                                //通过Iterator（迭代器）遍历Set（集）
                    //没有遍历完的话
                    while(iterator.hasNext()){
                        //将遍历得到的对象强制转换为SelectionKey类型
                        SelectionKey selectionKey = (SelectionKey)iterator.next();      
                                                                                //每次只访问一个对象
                        iterator.remove();                                      //访问过的对象被移走
                        //无效对象时阻塞
                        if(!selectionKey.isValid())
                            continue;   
                        //如果来的是读请求（从channel读数据）
                        else if(selectionKey.isReadable())
                            readFromChannel(selectionKey);
                        else break;
                    }
                }
            }catch (IOException io){}
        }
    }
    
    void readFromChannel(SelectionKey selectionKey) throws IOException{
        //获取selectionKey关联的channel，并强制转化为SocketChannel对象
        SocketChannel socketChannel = (SocketChannel)selectionKey.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(512);                       //分配512字节的字节缓冲区
        byteBuffer.clear();                                                     //首先清空缓冲区
        String message = "";                            
        //将socketChannel中的数据（客户端用charset指定的编码法案编码后的字符串）读到byteBuffer中
        //返回读到socketChannel中的字节数
        //int bufferlength = socketChannel.read(byteBuffer);
        
        //重要！不能先用int bufferlength = socketChannel.read(byteBuffer);再用while(bufferlength > 0){}来获取通道中的消息
        //因为byteBuffer可能暂时没拿到消息，这样处理会遗漏掉通道中的消息
        //所以必须采取下面的方法，将取数据与判断取到没有的操作同时作为循环条件，就一定不会漏掉通道中的消息
        while(socketChannel.read(byteBuffer) > 0){
            byteBuffer.flip();                                                  //byteBuffer转变到读状态，即可从byteBuffer中读数据
            //将byteBuffer中的数据（用charset指定的编码法案编码后的字符串）用同一编码方案解码，添加到message中
            message += charset.decode(byteBuffer);
        } 
        //在客户端显示服务器发来的消息
        System.out.println(message);
        historyArrayList.add(message);
        //将此对应的channel设置为准备接受其他客户端请求
        selectionKey.interestOps(SelectionKey.OP_READ);
    }
    
    public static void main(String[] args) throws IOException
    {
        new ChatroomClient().initClient();
    }
    
}