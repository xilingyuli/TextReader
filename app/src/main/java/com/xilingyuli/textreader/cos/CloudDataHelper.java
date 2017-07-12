package com.xilingyuli.textreader.cos;


import com.tencent.cos.model.COSRequest;
import com.tencent.cos.model.DeleteObjectRequest;
import com.tencent.cos.model.ListDirRequest;
import com.tencent.cos.model.MoveObjectRequest;
import com.tencent.cos.model.PutObjectRequest;
import com.tencent.cos.task.listener.ITaskListener;

import java.io.File;

/**
 * Created by xilingyuli on 2017/3/11.
 */

public class CloudDataHelper{

    public static final String ACTION_UPLOAD_FILE = "ACTION_UPLOAD_FILE";
    public static final String ACTION_LIST_FILE = "ACTION_LIST_FILE";
    public static final String ACTION_DELETE_FILE = "ACTION_DELETE_FILE";

    public static COSRequest createCOSRequest(Object... params){
        String action = (String) params[0];
        switch (action){
            case ACTION_UPLOAD_FILE:
                return createUpdateObjectRequest((ITaskListener)params[1], (String) params[2], (File)params[3], true);
            case ACTION_LIST_FILE:
                return createListDirRequest((ITaskListener)params[1], (String) params[2], (String) params[3]);
        }
        return null;
    }

    private static PutObjectRequest createUpdateObjectRequest(ITaskListener listener, String dir, File file, boolean keepName) {
        if(file==null||!file.exists()||!file.isFile()||!file.canRead())
            return null;
        String fileName = file.getName();
        String newFileName = System.currentTimeMillis()
                +(fileName.contains(".")?fileName.substring(fileName.indexOf(".")):"");
        String cosPath = dir + "/" + (keepName?fileName:newFileName);
        String signature = CloudDataUtil.sign(false, "");

        PutObjectRequest putObjectRequest = new PutObjectRequest();
        putObjectRequest.setBucket(CloudDataUtil.bucket);
        putObjectRequest.setCosPath(cosPath);
        putObjectRequest.setSrcPath(file.getPath());
        putObjectRequest.setSign(signature);
        putObjectRequest.setInsertOnly("0");
        putObjectRequest.setListener(listener);
        return putObjectRequest;
    }

    private static ListDirRequest createListDirRequest(ITaskListener listener, String dir, String content) {
        String signature = CloudDataUtil.sign(false, "");
        ListDirRequest listDirRequest = new ListDirRequest();
        listDirRequest.setBucket(CloudDataUtil.bucket);
        listDirRequest.setCosPath(dir);
        listDirRequest.setNum(100);
        listDirRequest.setContent(content);
        listDirRequest.setSign(signature);
        listDirRequest.setListener(listener);
        return listDirRequest;
    }

    private static MoveObjectRequest createMoveObjectRequest(ITaskListener listener, String dir, String oldName, String newName) {
        String cosPath = "/"+dir+"/"+oldName;
        String fullPath = "/"+CloudDataUtil.appId+"/"+CloudDataUtil.bucket+cosPath;

        MoveObjectRequest moveObjectRequest = new MoveObjectRequest();
        moveObjectRequest.setBucket(CloudDataUtil.bucket);
        moveObjectRequest.setCosPath(cosPath);
        moveObjectRequest.setDest_Filed(newName);
        moveObjectRequest.setSign(CloudDataUtil.sign(true, fullPath));
        moveObjectRequest.setListener(listener);
        return moveObjectRequest;

    }

    private static DeleteObjectRequest createDeleteObjectRequest(ITaskListener listener, String dir, String name){
        String cosPath = "/"+dir+"/"+name;
        String fullPath = "/"+CloudDataUtil.appId+"/"+CloudDataUtil.bucket+cosPath;

        DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest();
        deleteObjectRequest.setBucket(CloudDataUtil.bucket);
        deleteObjectRequest.setCosPath(cosPath);
        deleteObjectRequest.setSign(CloudDataUtil.sign(true,fullPath));
        deleteObjectRequest.setListener(listener);
        return deleteObjectRequest;
    }

}
