package enshud.s1.lexer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Lexer {

	/**
	 * サンプルmainメソッド．
	 * 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// normalの確認
		new Lexer().run("data/pas/out.pas", "data/ts/out.ts");
		//new Lexer().run("data/pas/normal02.pas", "tmp/out2.ts");
		//new Lexer().run("data/pas/normal03.pas", "tmp/out3.ts");
	}

	/**
	 * TODO
	 *
	 * 開発対象となるLexer実行メソッド．
	 * 以下の仕様を満たすこと．
	 *
	 * 仕様:
	 * 第一引数で指定されたpasファイルを読み込み，トークン列に分割する．
	 * トークン列は第二引数で指定されたtsファイルに書き出すこと．
	 * 正常に処理が終了した場合は標準出力に"OK"を，
	 * 入力ファイルが見つからない場合は標準エラーに"File not found"と出力して終了すること．
	 *
	 * @param inputFileName 入力pasファイル名
	 * @param outputFileName 出力tsファイル名
	 */
	public void run(final String inputFileName, final String outputFileName) {

		// TODO
		Path file = Paths.get(inputFileName);
		String text = "";
		try {
			text = Files.readString(file);
		} catch(IOException ex) {
			System.err.println("File not found");
		}
 		String[][] alp= {{"and", "array", "begin", "boolean", "char", "div", "do", "else",
			"end" ,"false", "if", "integer", "mod", "not", "of", "or", "procedure",
			"program", "readln", "then", "true", "var", "while", "writeln"},
			{"SAND", "SARRAY", "SBEGIN", "SBOOLEAN", "SCHAR", "SDIVD", "SDO", "SELSE",
			"SEND","SFALSE", "SIF", "SINTEGER", "SMOD", "SNOT", "SOF", "SOR", "SPROCEDURE",
			"SPROGRAM", "SREADIN", "STHEN", "STRUE", "SVAR", "SWHILE", "SWRITELN"}};
		String[][] sym = {{"5", "/", "SDIVD"}, {"24", "=", "SEQUAL"},
				{"26", "<", "SLESS"}, {"29", ">", "SGREAT"}, {"30", "+", "SPLUS"},
				{"31", "-", "SMINUS"}, {"32", "*", "SSTAR"}, {"33", "(", "SLPAREN"},
				{"34", ")", "SRPAREN"}, {"35", "[", "SLBRACKET"}, {"36", "]", "SRBRACKET"},
				{"37", ";", "SSEMICOLON"}, {"38", ":", "SCOLON"}, {"41", ",", "SCOMMA"},
				{"42", ".", "SDOT"}};
		int symSize = sym.length;
		int start = 0;
		int end = 0;
	    int counter = 1;
	    int length = text.length();
	    int state;
	    StringBuilder buf = new StringBuilder();
	    int i = 0;
	    while (i < length){
	    	if(text.charAt(i) == '\b'||text.charAt(i) == '\t')
	    		state = 0;
	    	else if(text.charAt(i) == '\n')
	    		state = 1;
	    	else if(text.charAt(i) == '{')
	    		state = 2;
	    	else if(text.charAt(i) == '\'')
	    		state = 3;
	    	else if(Character.isLetter(text.charAt(i)))
	    		state = 4;
	    	else if(Character.isDigit(text.charAt(i)))
	    		state = 5;
	    	else
	    		state = 6;
	    	switch(state) {
	    	case 0:
	    		i++;
	    		break;
	    	case 1:
	    		i++;
	    		counter++;
	    		break;
	    	case 2:
	    		i++;
	    		while(i < length) {
	    			if(text.charAt(i) == '}')
	    				break;
	    			else if(text.charAt(i) == '\n')
	    				counter++;
	    			i++;
	    		}
	    		i++;
	    		break;
	    	case 3:
	    		start = i;
	    		i++;
	    		while(i < length) {
	    			if(text.charAt(i) == '\'')
	    				break;
	    			if(text.charAt(i) == '\n') {
	    				counter++;
	    				break;
	    			}
	    			i++;
	    		}
	    		end = i;
	    		i++;
	    		buf.append(text.substring(start, end));
	    		buf.append("\'\tSSTRING\t45\t");
	    		buf.append(counter);
	    		buf.append('\n');
	    		break;
	    	case 4:
	    		start = i;
	    		i++;
	    		while(i < length && Character.isLetterOrDigit(text.charAt(i))) {
	    			i++;
	    		}
	    		end = i;
	    		int answer = Arrays.binarySearch(alp[0], text.substring(start, end));
	    		if(answer >= 0) {
		    		buf.append(alp[0][answer]);
		    		buf.append("\t");
		    		buf.append(alp[1][answer]);
		    		buf.append("\t");
		    		buf.append(answer);
		    		buf.append("\t");
		    		buf.append(counter);
		    		buf.append('\n');
	    		}else {
		    		buf.append(text.substring(start, end));
		    		buf.append("\tSIDENTIFIER\t43\t");
		    		buf.append(counter);
		    		buf.append('\n');
	    		}
	    		break;
	    	case 5:
	    		start = i;
	    		i++;
	    		while(i < length && Character.isDigit(text.charAt(i))) {
	    			i++;
	    		}
	    		end = i;
	    		buf.append(text.substring(start, end));
	    		buf.append("\tSCONSTANT\t44\t");
	    		buf.append(counter);
	    		buf.append('\n');
	    		break;
	    	case 6:
	    		if(text.substring(i, i+2).equals("<>")) {
		    		buf.append(text.substring(i, i+2));
		    		buf.append("\tSNOTEQUAL\t25\t");
		    		buf.append(counter);
		    		buf.append('\n');
		    		i+=2;
	    		}else if(text.substring(i, i+2).equals("<=")) {
		    		buf.append(text.substring(i, i+2));
		    		buf.append("\tSLESSEQUAL\t27\t");
		    		buf.append(counter);
		    		buf.append('\n');
		    		i+=2;
	    		}else if(text.substring(i, i+2).equals(">=")) {
		    		buf.append(text.substring(i, i+2));
		    		buf.append("\tSGREATEQUAL\t28\t");
		    		buf.append(counter);
		    		buf.append('\n');
		    		i+=2;
	    		}else if(text.substring(i, i+2).equals("..")) {
		    		buf.append(text.substring(i, i+2));
		    		buf.append("\tSRANGE\t39\t");
		    		buf.append(counter);
		    		buf.append('\n');
		    		i+=2;
	    		}else if(text.substring(i, i+2).equals(":=")) {
		    		buf.append(text.substring(i, i+2));
		    		buf.append("\tSASSIGN\t40\t");
		    		buf.append(counter);
		    		buf.append('\n');
		    		i+=2;
	    		}else {
	    			for(int j = 0; j < symSize; j++) {
	    				if(text.substring(i, i+1).equals(sym[j][1])) {
	    		    		buf.append(sym[j][1]);
	    		    		buf.append("\t");
	    		    		buf.append(sym[j][2]);
	    		    		buf.append("\t");
	    		    		buf.append(sym[j][0]);
	    		    		buf.append("\t");
	    		    		buf.append(counter);
	    		    		buf.append('\n');
	    		    		break;
	    				}
	    			}
	    			i++;
	    		}
	    		break;
	    	}
	    }
        try {
            FileWriter file2 = new FileWriter(outputFileName);
            PrintWriter pw = new PrintWriter(new BufferedWriter(file2));

            pw.print(buf);

            pw.close();

            System.out.println("OK");
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
