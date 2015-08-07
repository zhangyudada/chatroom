package chatroom;

//包是在编程过程中自动加上的                                                              
import java.io.IOException;                                                     //标准IO包
import java.net.InetSocketAddress;                                              //InetSocketAddress()方法所在
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
//import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author zhangyu
 */
public class ChatroomServer {
    
    Selector selector                           = null;                         //选择器，全局的
    private final int port                      = 12345;                        //服务器端的端口
    private final Charset charset               = Charset.forName("UTF-8");     //统一采用UTF-8(Unicode)编码和解码，避免出现乱码
    //HashSet类按照哈希算法来存取集合中的对象，存取速度比较快，这里存字符串数组
    //userHashSet存储客户端的套接字、登录状态以及用户名（登录后才有）
    private HashSet userHashSet                 = new HashSet();
    //由数组实现的List，允许对元素进行快速随机访问，存放String类型的历史消息
    //发现由服务器维护每个客户端的消息记录比较麻烦，就把这个消息记录的维护交给客户端
    //private ArrayList<String> historyArrayList  = new ArrayList<String>();
    
    //初始化服务器
    public void initServer() throws IOException{ 
        selector = Selector.open();                                             //新建选择器对象，控制各种channel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();   //新建服务器端通道，监听新进来的TCP连接
                                                                                //类似于标准IO中的ServerSocket
        serverSocketChannel.bind(new InetSocketAddress(port));                  //服务器端端口由最终变量port指定
        serverSocketChannel.configureBlocking(false);                           //与Selector共同作用时，必须设为非阻塞监听模式        
        //将serverSocketChannel注册到selector，注册事件为监听连接请求
        //返回一个SelectionKey类的对象（这里没有必要使用这个对象，故没有对象名）
        //这个对象联系着serverSocketChannel和selector
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("ChatroomSever is working..." + new Date());
        judge(serverSocketChannel);    
    }
    
    //判断请求类型并作相关处理
    void judge(ServerSocketChannel serverSocketChannel) throws IOException{
        while(true){                                                            //开始监听           
            //接受各种请求，然后判断请求的类型并作出相应处理
            int readyChannels = selector.select();                              //返回值为就绪的通道数
            if(readyChannels == 0) continue;                                    //没有通道就绪就接着阻塞
            Set selectedKeys = selector.selectedKeys();                         //将就绪通道的键放到一个Set（集）对象中
            Iterator iterator = selectedKeys.iterator();                        //Set类的方法iterator()返回一个Iterator对象
                                                                                //通过Iterator（迭代器）遍历Set（集）
            //没有遍历完的话
            while(iterator.hasNext()){
                SelectionKey selectionKey = (SelectionKey)iterator.next();      //将遍历得到的对象强制转换为SelectionKey类型
                                                                                //每次只访问一个对象
                iterator.remove();                                              //访问过的对象被移走
                //无效对象时阻塞
                if(!selectionKey.isValid())
                    continue;                           
                //如果来的是连接请求
                else if(selectionKey.isAcceptable())
                    accept(serverSocketChannel,selectionKey);
                //如果来的是读请求（从channel读数据）
                else if(selectionKey.isReadable())
                    readFromChannel(selectionKey);
                else break;
            }
        }
    }
    
    //处理来自客户端的连接请求
    void accept(ServerSocketChannel serverSocketChannel,SelectionKey selectionKey) throws IOException{
        //获取selectionKey关联的channel，并强制转化为ServerSocketChannel对象
        //ServerSocketChannel serverSocketChannel = (ServerSocketChannel)selectionKey.channel();
        //新建一个SocketChannel对象，设置为非阻塞模式，注册到selector上，注册事件为读事件
        //今后由这个socketChannel来处理发送此连接请求的客户端数据
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);                 //返回SelectionKey类的对象
        //将此对应的channel设置为准备接受其他客户端请求
        selectionKey.interestOps(SelectionKey.OP_ACCEPT);
        System.out.println("Establish a connection with client "+socketChannel.getRemoteAddress()); 
        //向socketChannel中写入提示登录的消息（用charset指定的编码法案编码）
        //这样客户端就会收到这个编码后的字符串
        socketChannel.write(charset.encode("Please login"));
        //申请长度为3的字符串数组，存放当前客户端的套接字、登录状态以及用户名（登录后才有），并加到userHashSet集中
        String[] state = new String[3];
        state[0] = socketChannel.getRemoteAddress().toString();                 //当前客户端套接字
        state[1] = "0";                                                         //当前客户端登录状态
        state[2] = "";                                                          //当前客户端用户名
        userHashSet.add(state);           
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
        
        //如果有已连接客户端（可能是已登录的也可能是未登录的）强制退出（按红色的stop按钮）
        //再使用socketChannel.read(byteBuffer)就会抛出java.io.IOException
        //所以这里需要进行异常处理
        try{
            //重要！不能先用int bufferlength = socketChannel.read(byteBuffer);再用while(bufferlength > 0){}来获取通道中的消息
            //因为byteBuffer可能暂时没拿到消息，这样处理会遗漏掉通道中的消息
            //所以必须采取下面的方法，将取数据与判断取到没有的操作同时作为循环条件，就一定不会漏掉通道中的消息
            while(socketChannel.read(byteBuffer) > 0){
                byteBuffer.flip();                                                  //byteBuffer转变到读状态，即可从byteBuffer中读数据
                //将byteBuffer中的数据（用charset指定的编码法案编码后的字符串）用同一编码方案解码，添加到message中
                message += charset.decode(byteBuffer);
            } 
            System.out.println("message from"+socketChannel.getRemoteAddress()+":"+message);
            //将此对应的channel设置为准备下一次接受数据
            selectionKey.interestOps(SelectionKey.OP_READ);
            dealwithMessage(selectionKey,socketChannel,message);
        }catch(IOException e){
            abnormalQuit(selectionKey,socketChannel);  
        }
    }
    
    //服务器端接收到消息后进行消息识别和处理，服务器的核心功能
    void dealwithMessage(SelectionKey selectionKey,SocketChannel socketChannel,String message) throws IOException{
        //客户端未登录
        if(!isLogin(socketChannel)){
            if(message.length() > 0){
                //将message以空格分隔再存到数组msgArray中
                String[] msgArray = message.split(" ");
                switch(msgArray[0]){
                    case "/login"   : login(socketChannel,msgArray)         ;break;
                    case "/quit"    : logoutQuit(selectionKey,socketChannel);break; 
                    //未登录状态下输入其他消息要向客户端报错
                    default         : socketChannel.write(charset.encode("Ivalid command"));
                }                      
            }
//            //自加功能，发送空消息（直接回车或若干空格）的话服务器会提示你不要再发了
//            //不过由于客户端已设置不允许发空消息，所以这个判断也没什么用了，姑且留着
//            else
//                socketChannel.write(charset.encode("Do not send a blank message!"));
        }
        //客户端已登录
        else{
            if(message.length() > 0){
                //由于客户端已经不允许只发送空格了
                //所以这里以空格分隔消息再存到数组msgArray中后
                //数组msgArray肯定有元素，引用msgArray[0]就不会产生数组下标越界异常ArrayIndexOutOfBoundsException
                //所以也就不用做异常处理了
                String[] msgArray = message.split(" ");
                switch(msgArray[0]){
                    case "/quit"    : loginQuit(selectionKey,socketChannel);break;
                    case "//smile"  : smile(socketChannel);                 break;
                        
                    //自加的一些预设消息，发挥娱乐精神
                    case "//angry"  : angry(socketChannel);                 break;
                    case "//sad"    : sad(socketChannel);                   break;
                    case "//shock"  : shock(socketChannel);                 break;
                        
                    case "//hi"     : hi(socketChannel,msgArray);           break;
                    case "/to"      : to(socketChannel,msgArray);           break;
                    case "/who"     : who(socketChannel);                   break;
                    //历史消息查询交给客户端了
                    //case "/history" : history(msgArray);break;
                    //其他情况都视为广播消息
                    default         :
                        socketChannel.write(charset.encode("你说："+message));
                        String yourName = getUserName(socketChannel);
                        broadCastExcept(socketChannel,yourName+"说："+message);
                        break;
                } 
            }
//            //自加功能，发送空消息（直接回车或若干空格）的话服务器会提示你不要再发了
//            //不过由于客户端已设置不允许发空消息，所以这个判断也没什么用了，姑且留着
//            else
//                socketChannel.write(charset.encode("Do not send a blank message!"));
        }
    }
    
    //已连接客户端（可能是已登录的也可能是未登录的）强制退出的处理方法
    void abnormalQuit(SelectionKey selectionKey,SocketChannel socketChannel) throws IOException{
        //客户端未登录
        if(!isLogin(socketChannel)){
            //调用未登录状态下的退出处理方法来关闭这条连接
            logoutQuit(selectionKey,socketChannel);
        }
        //客户端已登录
        else{
            //调用已登录状态下的退出处理方法来广播其他在线用户并关闭这条连接
            loginQuit(selectionKey,socketChannel);
        }
    }

    //根据userHashSet中用户信息，判断当前客户端是否已经登录
    //如果socketChannel可以存放客户端的登录信息，则直接读出这个登录信息即可
    //但是由于socketChannel中没有存放客户端的登录信息，所以只好采用这种循环查询的方法来查询客户端是否已经登录
    boolean isLogin(SocketChannel socketChannel) throws IOException{
        boolean isLogin = false;
        //由迭代器iterator遍历userHashSet
        Iterator iterator = userHashSet.iterator();
        while(iterator.hasNext()){
            String[] state = (String[])iterator.next();                         //强制转换为字符串数组
            //state[0]中存放的是客户端的套接字信息，这里查询当前客户端对应的用户信息记录
            if(!state[0].equals(socketChannel.getRemoteAddress().toString()))
                continue;
            //state[1]中存放客户端的登录信息，非"0"（一般设为"1"）表示已登录
            if(!state[1].equals("0"))
                isLogin = true;                                                
        }
        return isLogin;
    }
    
    //处理客户端登录请求
    void login(SocketChannel socketChannel,String[] msgArray) throws IOException{
        boolean nameExist     = false; 
        String userName       ="";

        //如果客户端提交的用户名为空，则不会有msgArray[1]
        //即抛出数组下标越界异常ArrayIndexOutOfBoundsException，需要做异常处理
        try{
            userName          = msgArray[1];                                    //当前客户端提交的用户名
        }catch(ArrayIndexOutOfBoundsException e){
            socketChannel.write(charset.encode("Username can not be empty!"));
        }
        
        //查询到当前客户端的信息后存放在currentState中
        String[] currentState = new String[3];
        //用户名非空才合法
        if(!userName.equals("")){
            Iterator iterator = userHashSet.iterator();
            while(iterator.hasNext()){
                String[] state = (String[])iterator.next();                     //强制转换为字符串数组
                //取得对应当前客户端的信息记录，避免插入用户名信息时再查询一遍
                //这是个引用赋值，通过改变currentState即可改变state从而改变userHashSet中的对应记录
                //查询到当前客户端，则userHashSet中对应记录肯定没有用户名，故用continue直接查询下一条记录
                if(state[0].equals(socketChannel.getRemoteAddress().toString())){
                    currentState = state;
                    continue;
                }                   
                //state[2]中存放的是客户端的用户名，这里查询当前用户名是不是已经存在
                //若用户名存在，则设置标志nameExist位true，并跳出循环
                if(state[2].equals(userName)){
                    nameExist = true;
                    break;
                }                                       
            }
            //服务器上没有同名用户，将用户名记录下来，并发出相应消息到客户端
            if(!nameExist){
                currentState[1] = "1";                                          //记录用户为已登录
                currentState[2] = userName;
                //发给当前用户
                socketChannel.write(charset.encode("You have logined"));
                //发给除当前用户外的其他在线用户
                broadCastExcept(socketChannel,userName+" has logined");
            }
            //服务器上有同名用户,则通知当前客户端
            else
                socketChannel.write(charset.encode("Name exist, please choose anthoer name."));                
        }
//        //自加功能，用户名不允许为空
//        //不过由于客户端已设置不允许发空的用户名，所以这个判断也没什么用了
//        else
//            socketChannel.write(charset.encode("Do not send a blank message!"));        
    }
    
    //未登录状态下直接退出
    void logoutQuit(SelectionKey selectionKey,SocketChannel socketChannel) throws IOException{ 
        System.out.println("某个已连接但未登录用户断开了连接");
        //关闭连接
        selectionKey.cancel();
        socketChannel.close();
        socketChannel.socket().close();
    }
    
    //登录状态下的退出
    void loginQuit(SelectionKey selectionKey,SocketChannel socketChannel) throws IOException{
        String userName = "";       
        //关闭连接前先清空userHashSet中的客户端信息
        Iterator iterator = userHashSet.iterator();
            while(iterator.hasNext()){
                String[] state = (String[])iterator.next();                     //强制转换为字符串数组
                if(state[0].equals(socketChannel.getRemoteAddress().toString())){
                    userName = state[2];
                    //移除userHashSet中退出的客户端信息记录
                    iterator.remove();
                    break;
                }
            }
        System.out.println(userName + " has quit.");
        broadCastExcept(socketChannel,userName+" has quit.");                   //广播退出的消息
        //关闭连接
        selectionKey.cancel();
        socketChannel.close();
        socketChannel.socket().close();   
    }
    
    //预设消息"//smile"的处理
    void smile(SocketChannel socketChannel) throws IOException{
        String yourName = getUserName(socketChannel);
            broadCastExcept(socketChannel,yourName+"脸上泛起无邪的笑容");
            socketChannel.write(charset.encode("你脸上泛起无邪的笑容"));
    }
    
    //预设消息"//angry"的处理
    void angry(SocketChannel socketChannel) throws IOException{
        String yourName = getUserName(socketChannel);
            broadCastExcept(socketChannel,yourName+"很生气，后果很严重");
            socketChannel.write(charset.encode("你很生气，后果很严重"));
    }
    
    //预设消息"//sad"的处理
    void sad(SocketChannel socketChannel) throws IOException{
        String yourName = getUserName(socketChannel);
            broadCastExcept(socketChannel,yourName+"表示这真是个悲伤的故事");
            socketChannel.write(charset.encode("你表示这真是个悲伤的故事"));
    }
    
    //预设消息"//shock"的处理
    void shock(SocketChannel socketChannel) throws IOException{
        String yourName = getUserName(socketChannel);
            broadCastExcept(socketChannel,yourName+"完全被震惊了");
            socketChannel.write(charset.encode("你完全被震惊了"));
    }
    
    //预设消息"//hi"的处理
    void hi(SocketChannel socketChannel,String[] msgArray) throws IOException{
        String yourName = getUserName(socketChannel);
        //"//hi"后面没有参数时，则msgArray长度为1
        if(msgArray.length == 1){
            broadCastExcept(socketChannel,yourName+"向大家打招呼，“Hi，大家好！我来咯~”");
            socketChannel.write(charset.encode("你向大家打招呼，“Hi，大家好！我来咯~”"));
        }
        else if(msgArray.length > 1){
            //"//hi"后面紧跟着的字符串就是其他用户的用户名
            String userName = msgArray[1];
            broadCastExcept(socketChannel,yourName+"向"+userName+"打招呼：“Hi，你好啊~”");
            socketChannel.write(charset.encode("你向"+userName+"打招呼：“Hi，你好啊~”"));
        }
    }
    
    //私聊指令的处理
    void to(SocketChannel socketChannel,String[] msgArray) throws IOException{
        String yourName          = getUserName(socketChannel);                  //获得当前客户端用户名
        String userRemoteAddress = "";                                          //存放目标客户端套接字
        String message           = "";                                          //待发送消息
        boolean userNameExist    = false;                                       //判断用户名是否存在
        //如果指令"/to"后面什么也没有了，则提示用户指定私聊用户
        if(msgArray.length == 1)
            socketChannel.write(charset.encode("请在/to后面指定私聊用户名"));
        else if(msgArray.length > 1){
            //"/to"后面紧跟着的字符串就是其他用户的用户名
            String userName = msgArray[1];           
            //如果user_name是用户自己则提示用户不要和自己对话
            if(userName.equals(yourName))
                socketChannel.write(charset.encode("Stop talking to yourself!"));
            //如果user_name不是用户自己
            else{
                Iterator iterator = userHashSet.iterator();
                while(iterator.hasNext()){
                    String[] state = (String[])iterator.next();                 //强制转换为字符串数组
                    //如果能在userHashSet中找到指定用户，则记录这个用户的套接字
                    if(state[2].equals(userName)){                              //state[2]存有字符串形式的客户端用户名
                        userRemoteAddress = state[0];                           //state[0]存有字符串形式的客户端套接字
                        userNameExist = true;
                        break;
                    }
                }
                //如果在userHashSet中找不到指定用户，则显示：user_name is not online.
                if(!userNameExist)
                    socketChannel.write(charset.encode(userName+" is not online."));
                //在userHashSet中找到指定用户
                else{ 
                    //foreach的语句格式：for(元素类型t 元素变量x : 遍历对象obj)，适合于遍历
                    //在selectionKey中找到带目标套接字所对应的socketChannel
                    for(SelectionKey selectionKey : selector.keys()){            
                        Channel targetChannel = selectionKey.channel();
                        if(targetChannel instanceof SocketChannel){
                            SocketChannel otherChannel = (SocketChannel)targetChannel;
                            //获得客户端的套接字
                            String address = otherChannel.getRemoteAddress().toString();
                            //客户端套接字与目标套接字相同，则找到目标
                            if(address.equals(userRemoteAddress)){
                                //message为msgArray除去前两个元素（控制命令和用户名）以外的所有字符串
                                for(int i = 2;i < msgArray.length;i++)
                                    message += msgArray[i];                               
                                socketChannel.write(charset.encode("你对"+userName+"说："+message));
                                otherChannel.write(charset.encode(yourName+"对你说："+message));
                                break;                                          //找到指定用户后就不用再找了
                            }                        
                        }
                    }
                }               
            }           
        }
    }
    
    //查询指令的处理
    void who(SocketChannel socketChannel) throws IOException{
        int userNum = 0;
        //向客户端写入提示消息并换行
        socketChannel.write(charset.encode("当前在线用户为：\n"));
        //遍历userHashSet
        Iterator iterator = userHashSet.iterator();
            while(iterator.hasNext()){               
                String[] state = (String[])iterator.next();                     //强制转换为字符串数组
                //state[2]存放的是用户名，若非空，则说明是登录用户，需要显示
                if(!state[2].equals("")){
                    socketChannel.write(charset.encode(state[2]+"\n"));         //列出用户名，一人一行
                    userNum++;
                }                   
            }
        //遍历完了之后在客户端显示总在线人数
        socketChannel.write(charset.encode("Total online user: "+userNum));    
    }
    

//    void history(String[] msgArray){
//        
//    }
   
    //获得socketChannel对应的用户名
   String getUserName(SocketChannel socketChannel) throws IOException{
       //获取socketChannel对应客户端的用户名
        String userName = "";
        Iterator iterator = userHashSet.iterator();
            while(iterator.hasNext()){
                String[] state = (String[])iterator.next();                     //强制转换为字符串数组
                if(state[0].equals(socketChannel.getRemoteAddress().toString())){
                    userName = state[2];
                    break;
                }
            }
        return userName;
   }
    
    //将message广播到除oneself外的所有已经登录的SocketChannel
    void broadCastExcept(SocketChannel oneself,String message) throws IOException{
        //foreach的语句格式：for(元素类型t 元素变量x : 遍历对象obj)，适合于遍历
        //遍历selector中的SelectionKey对象
        for(SelectionKey selectionKey : selector.keys()){            
            Channel targetChannel = selectionKey.channel();
            //目标客户端为除了本身外的其他所有客户端
            if(targetChannel instanceof SocketChannel && targetChannel != oneself){
                SocketChannel otherChannel = (SocketChannel)targetChannel;
                //只向已登录用户广播消息
                if(isLogin(otherChannel))
                    otherChannel.write(charset.encode(message));
            }
        }
    }
    
    public static void main(String[] args) throws IOException{
        new ChatroomServer().initServer();
    }
    
}