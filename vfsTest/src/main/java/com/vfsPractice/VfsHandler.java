package com.vfsPractice;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VfsHandler {

	private static final Logger log = LoggerFactory.getLogger(VfsHandler.class);
	
	private String username = "SFTPaccount";
	private String password = "Password";
	private String remoteHost = "192.168.0.0:22";
	private String remoteDir = "/upload";
	private static final String DIRECTORY_FOR_SUCCESS = "done";
    private static final String DIRECTORY_FOR_FAIL = "except";
    int numberOfFile = 2;
    int fileSize = 5;

	
	@SuppressWarnings("resource")
	@Scheduled(fixedDelayString = "${targos.interval.check-rsa-session-ids:60000}", initialDelay = 1000)
	public void DownloadFile() throws IOException {

		FileSystemManager manager = VFS.getManager();

		String sftpUrl = "sftp://" + username + ":" + password + "@" + remoteHost + remoteDir + "/";
		String localDir = System.getProperty("user.dir") + "/src/main/resources/";


		FileObject local = manager.resolveFile(localDir);
		FileObject sftpServer = manager.resolveFile(sftpUrl);
		FileObject successFolder = manager.resolveFile(sftpUrl + "/" + DIRECTORY_FOR_SUCCESS);
		FileObject failFolder = manager.resolveFile(sftpUrl + "/" + DIRECTORY_FOR_FAIL);

		try {
		    successFolder.createFolder();
		} catch (Exception e) {
		    log.error("Unable to create a success folder " + e);
		}

		try {
		    failFolder.createFolder();
		} catch (Exception e) {
		    log.error("Unable to create a fail folder " + e);
		}

		// 지정 폴더 내의 리스트
		FileObject[] fileList = sftpServer.getChildren();

		List<FileObject> orderedList = sort(fileList);

		int count = 0;
		for (FileObject file : orderedList) {
		    if (count >= numberOfFile) {
			break;
		    }

		    if (!file.isFolder() && file.exists()) {
			String fileName = file.getName().getBaseName();

			try {

			    // (bytes -> MB 변환 후 비교)
			    if (file.getContent().getSize() <= fileSize * 1024 * 1024) {

				// 파일 중복 확인 (중복일 경우 파일이름_날짜 로 저장)
				if (local.exists()) {
				    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
				    String extension = "." + file.getName().getExtension();
				    fileName = fileName.substring(0, fileName.lastIndexOf(extension)) + "_" + time + extension;
				}
				local = manager.resolveFile(localDir + fileName);
				local.copyFrom(file, Selectors.SELECT_SELF);

				sftpServer = manager.resolveFile(file.toString());
				sftpServer.moveTo(manager.resolveFile(successFolder + fileName));
				count++;

			    } else {
				// 사이즈 제한에 따른 수집 예외 파일
				sftpServer.moveTo(manager.resolveFile(failFolder + fileName));
				count++;
			    }

			} catch (Exception e) {
			    log.error("DownloadOriginalFile fail: " + e);
			}
		    }
		}

		local.close();
		sftpServer.close();
	    }
	
	//파일 수정일 기준 정렬
    @SuppressWarnings("resource")
    private List<FileObject> sort(FileObject[] fileObjects) {
	List<FileObject> orderedList = new ArrayList<>();

	for (FileObject file : fileObjects) {

	    FileObject oldest = file;
	    for (FileObject compareFile : fileObjects) {
		try {
		    if (oldest.getContent().getLastModifiedTime() > compareFile.getContent().getLastModifiedTime()
			    && !orderedList.contains(compareFile)) {
			oldest = compareFile;
		    } else if (orderedList.contains(oldest)) {
			if (!orderedList.contains(compareFile)) {
			    oldest = compareFile;
			} else {
			    continue;
			}
		    }
		} catch (FileSystemException e) {
		    log.error("" + e);
		}
	    }

	    orderedList.add(oldest);

	}

	return orderedList;
    }
}
