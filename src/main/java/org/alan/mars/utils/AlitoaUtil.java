package org.alan.mars.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created on 2018/2/10.
 *
 * @author Alan
 * @since 1.0
 */
public class AlitoaUtil {
    public static String getIp(String ip, int port) {
        //传入0表示让操作系统分配一个端口号
        try (DatagramSocket socket = new DatagramSocket(0)) {
            socket.setSoTimeout(1000);
            InetAddress host = InetAddress.getByName("127.0.0.1");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.write(ipToBytes(ip), 0, 4);
            dos.write(intToBytes(port), 2, 2);
            dos.writeInt(0);
            dos.writeShort(0);
            dos.flush();
            //指定包要发送的目的地
            DatagramPacket request = new DatagramPacket(baos.toByteArray(), 12, host, 48888);
            //为接受的数据包创建空间
            DatagramPacket response = new DatagramPacket(new byte[12], 12);
            socket.send(request);
            socket.receive(response);
            return bytesToIp(response.getData());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ip;
    }

    public static byte[] ipToBytes(String ipAddress) {
        byte[] bs = new byte[4];
        String[] ipAddressInArray = ipAddress.split("\\.");
        for (int i = 0; i < 4; i++) {
            bs[i] = new Integer(Short.parseShort(ipAddressInArray[i]) & 0xff).byteValue();
        }
        return bs;
    }

    /**
     * 注释：int到字节数组的转换！
     */
    public static byte[] intToBytes(int number) {
        int temp = number;
        byte[] b = new byte[4];
        for (int i = 0; i < b.length; i++) {
            b[3 - i] = new Integer(temp & 0xff).byteValue();//
            //将最低位保存在最低位
            temp = temp >> 8; // 向右移8位
        }
        return b;
    }

    public static String bytesToIp(byte[] data) {
        //System.out.println(Arrays.toString(data));
        return Byte.toUnsignedInt(data[6]) +
                "." + Byte.toUnsignedInt(data[7]) +
                "." + Byte.toUnsignedInt(data[8]) +
                "." + Byte.toUnsignedInt(data[9]);
    }
}
