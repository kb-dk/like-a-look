#!/usr/bin/env wolframscript
(*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *)
begin = Now;

(* CONFIGURATION *)
imageCollectionPlace = "/Users/pmd/KB/GTD/Projekter@KB/DANER/data/itusus";
extension = "png";

noFaceFile   = "images-without-a-face-"<>StringReplace[DateString["ISODateTime"],":"->"."]<>".txt";
dumpSaveFile = "extracted-features-"<>StringReplace[DateString["ISODateTime"],":"->"."]<>".mx";
(* END OF CONFIGURATION *)

(* BEGIN PROGRAM *)
logString[s_String] := Module[
  {len = StringLength@s},
  "##"
  <> StringRepeat[" ", 38 - Floor[len/2]]
  <> s
  <> StringRepeat[" ", 38 - Ceiling[len/2]]
  <> "##"
]

Print[StringRepeat["#",80]]
Print[logString@""]
Print[logString@"** BEGIN PROGRAM: FACE FEATURES EXTRACTOR **"]
Print[logString[DateString[]]]
Print[logString@""]
Print[logString@"CONFIGURATION"]
Print[logString@""]
Print["## Extract features from "<>extension<>" files in "<>imageCollectionPlace<>"."]
Print["## Save the names of files with no face in "<>noFaceFile<>"."]
Print["## Save a binary representation of the extracted features in "<>dumpSaveFile<>"."]
Print[logString@""]
Print[StringRepeat["#",80]]

imageCollectionFiles = FileNames["*."<>extension, imageCollectionPlace, Infinity];

Print["## Extract faces from the "<>ToString@Length@imageCollectionFiles<>" image files, and re-sample them to 224x224"]

(*
In the following we save the faces as a list of tuples:
   <image filename>, <data about image file>,
where the data in sequence is one of:
  - list of found faces,
  - B/W version of first face,
  - features extracted from face
*)

i = Length@imageCollectionFiles;
images = {
   EchoLabel["Processing "<>ToString[i--]][#],
   FaceAlign[Import@#, Automatic, {224, 224}]
}& /@ imageCollectionFiles;

imagesWithoutFace = Select[images, Length@#[[2]] == 0&][[All,1]];
Print["## Found "<>ToString@Length@imagesWithoutFace<>" images with no face."]
Print["## These images is stored in "<>noFaceFile<>"."]
Export[noFaceFile, imagesWithoutFace];

Print["## Convert the first face found in each image to B/W."]
faces = {
   EchoLabel["Processing "][#[[1]]],
   ColorConvert[
      First@#[[2]],
      "Grayscale"]
}& /@ Select[images, Length@#[[2]] > 0&];

Print@"## Load ResNet-101 Trained on Augmented CASIA-WebFace Data."
EchoTiming[featureExtractor = NetModel["ResNet-101 Trained on Augmented CASIA-WebFace Data"]]

i = Length@faces;
Print["## Extract features from the "<>ToString@Length@faces<>" faces."]
faceFeatures = {
  EchoLabel["Processing "<>ToString[i--]][#[[1]]],
  featureExtractor@#[[2]]
}& /@ faces;

Print["## Store the extracted features in "<>dumpSaveFile<>"."]
DumpSave[dumpSaveFile, faceFeatures]

Print[StringRepeat["#",80]]
Print[logString@""]
Print[logString["RUN TIME: "<>ToString[Now-begin]]]
Print[logString["EXTRACTED FEATURES FROM "<>ToString@Length@faces<>" FACES."]]
Print[logString["MEMORY IN USE: "<>ToString@NumberForm[MemoryInUse[], DigitBlock -> 3, NumberSeparator -> " "]<>" B."]]
Print[logString["FEATURES: "<>ToString@NumberForm[ByteCount[faceFeatures], DigitBlock -> 3, NumberSeparator -> " "]<>" B."]]
Print[logString@""]
Print[logString[DateString[]]]
Print[logString@"** END OF PROGRAM **"]
Print[logString@""]
Print[StringRepeat["#",80]]
