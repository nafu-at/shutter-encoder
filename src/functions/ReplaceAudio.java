/*******************************************************************************************
* Copyright (C) 2023 PACIFICO PAUL
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along
* with this program; if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
* 
********************************************************************************************/

package functions;

import java.io.File;

import application.Ftp;
import application.Settings;
import application.Shutter;
import application.Utils;
import application.VideoPlayer;
import application.Wetransfer;
import library.FFMPEG;
import library.FFPROBE;
import settings.FunctionUtils;
import settings.InputAndOutput;

public class ReplaceAudio extends Shutter {
	
	private static int shortestLength = 0;
	
	private static void main(String audioFiles, String audioExt, File videoFile) throws InterruptedException {
		
		if (scanIsRunning == false)
			FunctionUtils.completed = 0;
		
		lblFilesEnded.setText(FunctionUtils.completedFiles(FunctionUtils.completed));				

		String fileName = videoFile.getName();
		String extension =  fileName.substring(fileName.lastIndexOf("."));
		
		lblCurrentEncoding.setText(fileName);			
		
		//InOut	
		InputAndOutput.getInputAndOutput();		
		
		//Output folder
		String labelOutput = FunctionUtils.setOutputDestination("", videoFile);
		
		//File output name
		String extensionName = "_MIX";
		
		if (Settings.btnExtension.isSelected())
			extensionName = Settings.txtExtension.getText();
			
		//Output name
		String fileOutputName =  labelOutput.replace("\\", "/") + "/" + fileName.replace(extension, extensionName + extension); 

		//File output
		File fileOut = new File(fileOutputName);
		if(fileOut.exists())
		{
			fileOut = FunctionUtils.fileReplacement(labelOutput, fileName, extension, extensionName + "_", extension);
		}
		
		if (fileOut != null)
		{					
			String audio = setAudio(extension, audioExt);
			String shortest = " -t " + shortestLength + "ms";
			if (comboFilter.getSelectedItem().toString().equals(language.getProperty("longest")))
				shortest = "";
								
			//Command				
			String cmd = shortest + " -c:v copy -c:s copy" + audio + " -map s? -y ";
			FFMPEG.run(InputAndOutput.outPoint + " -i " + '"' + videoFile.toString() + '"' + audioFiles + cmd + '"'  + fileOut + '"');		
					
			do {
				Thread.sleep(100);
			} while(FFMPEG.runProcess.isAlive());
		
			if (FFMPEG.saveCode == false && btnStart.getText().equals(Shutter.language.getProperty("btnAddToRender")) == false)
			{
				lastActions(fileName, fileOut, labelOutput);
			}
					
		}
		
		if (btnStart.getText().equals(Shutter.language.getProperty("btnAddToRender")) == false)
			enfOfFunction();
		
    }

	public static void setStreams() {
		
		shortestLength = 0;
		
		Thread thread = new Thread(new Runnable() {	
			
			@Override
			public void run() {
				
				try {
				
					String audioFiles = null;	
					String audioExt = "";
					File videoFile = null;
					
					//Batch replace video analyze
					int videoStream = 0;							
					if (liste.getSize() > 2)
					{								
						for (int i = 0 ; i < liste.getSize() ; i++)
						{
							//Allows to get the shortest file duration
							FFPROBE.Data(liste.getElementAt(i));
							
							do {
								Thread.sleep(100);
							} while (FFPROBE.isRunning);
							
							if (FFPROBE.totalLength < shortestLength || shortestLength == 0)
								shortestLength = FFPROBE.totalLength;
							
							if (FFPROBE.FindStreams(liste.getElementAt(i)))
							{
								videoStream ++;
							}							
						}		
						
						//Start batch replace
						if (videoStream > 1)
						{
							for (int i = 0 ; i < liste.getSize() ; i++)
							{
								if (i % 2 == 0)
								{
									videoFile = new File(liste.getElementAt(i));
									
									if (comboAudioCodec.getSelectedItem().toString().equals(language.getProperty("noAudio")) && caseChangeAudioCodec.isSelected())
									{
										audioFiles = " -map v:0?";
									}								
									else if (caseChangeAudioCodec.isSelected())
									{
										audioFiles = " -map 0:v -map 0:a?";
									}
									else
									{
										audioFiles = " -i " + '"' + liste.getElementAt(i + 1)  + '"' + " -map 0:v -map 1:a";
										audioExt = liste.getElementAt(i + 1).substring(liste.getElementAt(i + 1).lastIndexOf("."));
									}
								}	
								else
								{
									if (comboAudioCodec.getSelectedItem().toString().equals(language.getProperty("noAudio")) && caseChangeAudioCodec.isSelected() || caseChangeAudioCodec.isSelected())
									{
										videoFile = new File(liste.getElementAt(i));
									}
									else									
									{
										continue;
									}
								}

								//Start replacement
								main(audioFiles, audioExt, videoFile);
								
								if (FFMPEG.error || Shutter.cancelled)
								{
									break;
								}
							}
						}
					}
					
					if (liste.getSize() <= 2 || videoStream == 1) //Replace one video file
					{
						if (comboAudioCodec.getSelectedItem().toString().equals(language.getProperty("noAudio")) == false || caseChangeAudioCodec.isSelected() == false)
						{		
							//Allows to get the shortest file duration
							FFPROBE.Data(liste.getElementAt(0));
							
							do {
								Thread.sleep(100);
							} while (FFPROBE.isRunning);
							
							shortestLength = FFPROBE.totalLength;
							
							FFPROBE.Data(liste.getElementAt(1));
							
							do {
								Thread.sleep(100);
							} while (FFPROBE.isRunning);
							
							if (FFPROBE.totalLength < shortestLength || shortestLength == 0)
								shortestLength = FFPROBE.totalLength;
							
							//Stream analyze		
							if (FFPROBE.FindStreams(liste.getElementAt(1)))
							{
								videoFile = new File(liste.getElementAt(1));
								audioFiles = " -i " + '"' + liste.getElementAt(0)  + '"';
								audioExt = liste.getElementAt(0).substring(liste.getElementAt(0).lastIndexOf("."));
								FFPROBE.FindStreams(liste.getElementAt(0));
							}
							else
							{
								videoFile = new File(liste.getElementAt(0));		
								audioFiles = " -i " + '"' + liste.getElementAt(1)  + '"';
								audioExt = liste.getElementAt(1).substring(liste.getElementAt(1).lastIndexOf("."));								
							}	
																					
							float offset = 0;
							
							if (caseAudioOffset.isSelected() || caseInAndOut.isSelected())
							{
								FFPROBE.Data(videoFile.toString());
								
								do {
									Thread.sleep(100);
								} while (FFPROBE.isRunning);
								
								if (caseAudioOffset.isSelected())
									offset = (float) ((float) Integer.parseInt(txtAudioOffset.getText()) * ((float) 1000 / FFPROBE.currentFPS)) / 1000;							
								else
									offset = (float) (Integer.parseInt(VideoPlayer.caseInH.getText()) * 3600 + Integer.parseInt(VideoPlayer.caseInM.getText()) * 60 + Integer.parseInt(VideoPlayer.caseInS.getText()) + ((float) Integer.parseInt(VideoPlayer.caseInF.getText()) * ((float) 1000 / FFPROBE.currentFPS)) / 1000);
								
								audioFiles = " -itsoffset " + offset + audioFiles;
							}
							
							audioFiles += " -map 0:v -map 1:a";
							
							if (liste.getSize() > 2)
								audioFiles = setMulipleAudioFiles(videoFile, "", offset);
															
							do {
								Thread.sleep(100);
							} while (FFPROBE.isRunning);	
						}
						else
						{
							videoFile = new File(liste.getElementAt(0));
							audioFiles = " -map v:0?";
						}	
						
						//Start replacement
						main(audioFiles, audioExt, videoFile);
					}
					
				} catch (InterruptedException e1) {
					FFMPEG.error  = true;
				}
			}
		});
		thread.start();
	}
	
	private static String setMulipleAudioFiles(File videoFile, String audioFiles, Float offset) {
		
		for (int i = 0 ; i < liste.getSize() ; i++)
		{
			if (liste.getElementAt(i).equals(" -f lavfi -i anullsrc=r=" + lbl48k.getText() + ":cl=mono")) //Si le fichier est une piste muette
			{
				audioFiles += liste.getElementAt(i) ;
			}
			else if (liste.getElementAt(i).equals(videoFile.toString()) == false) //Si le fichier n'est pas le fichier vidéo
			{
				if (caseAudioOffset.isSelected())
				{
					audioFiles += " -itsoffset " + offset + " -i " + '"' + liste.getElementAt(i)  + '"';
				}
				else if (caseInAndOut.isSelected())
				{
					offset = (float) (Integer.parseInt(VideoPlayer.caseInH.getText()) * 3600 + Integer.parseInt(VideoPlayer.caseInM.getText()) * 60 + Integer.parseInt(VideoPlayer.caseInS.getText()) + ((float) Integer.parseInt(VideoPlayer.caseInF.getText()) * ((float) 1000 / FFPROBE.currentFPS)) / 1000);
					audioFiles += " -itsoffset " + offset + " -i " + '"' + liste.getElementAt(i)  + '"';
				}
				else
					audioFiles += " -i " + '"' + liste.getElementAt(i)  + '"';
			}
	 	}
							
		audioFiles += " -map 0:v";
		
		for (int i = 1 ; i < liste.getSize() ; i++)
		{
			audioFiles +=  " -map " + i + ":a";
		}
		return audioFiles;
	}

	private static String setAudio(String ext, String audioExt) {
		
		if (caseChangeAudioCodec.isSelected())
		{
			if (comboAudioCodec.getSelectedItem().toString().contains("PCM"))
			{
				switch (comboAudioCodec.getSelectedIndex()) 
				{
					case 0 :
						return " -c:a pcm_f32le -ar " + lbl48k.getText() + " -b:a 1536k";
					case 1 :
						return " -c:a pcm_s32le -ar " + lbl48k.getText() + " -b:a 1536k";
					case 2 :
						return " -c:a pcm_s24le -ar " + lbl48k.getText() + " -b:a 1536k";
					case 3 :
						return " -c:a pcm_s16le -ar " + lbl48k.getText() + " -b:a 1536k";
				}
			}
			else if (comboAudioCodec.getSelectedItem().toString().equals("AAC"))
			{
				return " -c:a aac -ar " + lbl48k.getText() + " -b:a " + comboAudioBitrate.getSelectedItem().toString() + "k";
			}
			else if (comboAudioCodec.getSelectedItem().toString().equals("MP3"))
			{
				return " -c:a libmp3lame -ar " + lbl48k.getText() + " -b:a " + comboAudioBitrate.getSelectedItem().toString() + "k";
			}
			else if (comboAudioCodec.getSelectedItem().toString().equals("AC3"))
			{
				return " -c:a ac3 -ar " + lbl48k.getText() + " -b:a " + comboAudioBitrate.getSelectedItem().toString() + "k";
			}
			else if (comboAudioCodec.getSelectedItem().toString().equals("OPUS"))
			{
				return " -c:a libopus -ar " + lbl48k.getText() + " -b:a " + comboAudioBitrate.getSelectedItem().toString() + "k";
			}
			else if (comboAudioCodec.getSelectedItem().toString().equals("OGG"))
			{
				return " -c:a libvorbis -ar " + lbl48k.getText() + " -b:a " + comboAudioBitrate.getSelectedItem().toString() + "k";
			}
			else if (comboAudioCodec.getSelectedItem().toString().equals("Dolby Digital Plus"))
			{
				return " -c:a eac3 -ar " + lbl48k.getText() + " -b:a " + comboAudioBitrate.getSelectedItem().toString() + "k";
			}
			else //No audio
			{
				return " -an";
			}
		}
		else //Auto mode
		{
			if (audioExt.equals(".thd"))
			{
				return  " -c:a copy -strict -2";
			}
			
			switch (ext.toLowerCase()) 
			{			
				case ".mp4":
					if (audioExt.equals(".m4a") == false)
						return " -c:a aac -ar " + lbl48k.getText() + " -b:a 256k";
					else
						return  " -c:a copy";
				case ".mp3":
					if (audioExt.equals(".mp3") == false)
						return " -c:a libmp3lame -ar " + lbl48k.getText() + " -b:a 256k";
					else
						return  " -c:a copy";
				case ".wmv":
					if (audioExt.equals(".wma") == false)
						return " -c:a wmav2 -ar " + lbl48k.getText() + " -b:a 256k";
					else
						return  " -c:a copy";
				case ".mpg":
					if (audioExt.equals(".mp2") == false)
						return " -c:a mp2 -ar " + lbl48k.getText() + " -b:a 256k";
					else
						return  " -c:a copy";
				case ".ogv":
				case ".av1":
				case ".webm":
					return " -c:a libopus -ar " + lbl48k.getText() + " -b:a 192k";
			}
		}
		
		return  " -c:a copy";
	}
	
	private static void lastActions(String fileName, File fileOut, String output) {
		
		FunctionUtils.cleanFunction(fileName, fileOut, output);

		//Sending processes
		FunctionUtils.addFileForMail(fileName);
		Wetransfer.addFile(fileOut);
		Ftp.sendToFtp(fileOut);
		Utils.copyFile(fileOut);
		
	}

}
