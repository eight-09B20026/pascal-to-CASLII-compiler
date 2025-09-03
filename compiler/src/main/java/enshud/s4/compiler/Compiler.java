package enshud.s4.compiler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import enshud.casl.CaslSimulator;

public class Compiler {

	/**
	 * サンプルmainメソッド．
	 * 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// Compilerを実行してcasを生成する
		new Compiler().run("data/ts/out.ts", "data/cas/out.cas");

		// 上記casを，CASLアセンブラ & COMETシミュレータで実行する
		CaslSimulator.run("data/cas/out.cas", "data/ans/out.ans", "2023\n", "Y\n", "January\n", "");
	}

	/**
	 * TODO
	 *
	 * 開発対象となるCompiler実行メソッド．
	 * 以下の仕様を満たすこと．
	 *
	 * 仕様:
	 * 第一引数で指定されたtsファイルを読み込み，CASL IIプログラムにコンパイルする．
	 * コンパイル結果のCASL IIプログラムは第二引数で指定されたcasファイルに書き出すこと．
	 * 構文的もしくは意味的なエラーを発見した場合は標準エラーにエラーメッセージを出力すること．
	 * （エラーメッセージの内容はChecker.run()の出力に準じるものとする．）
	 * 入力ファイルが見つからない場合は標準エラーに"File not found"と出力して終了すること．
	 *
	 * @param inputFileName 入力tsファイル名
	 * @param outputFileName 出力casファイル名
	 */
	private Tokens tokens =new Tokens();
	private SymbolTable symbolTable = new SymbolTable();
	private SymbolTable subSymbolTable;
	private boolean isSub = false;
	private boolean isNew = false;
	private ArrayList<String> charTable = new ArrayList<>();
	private StringBuilder code = new StringBuilder();
	private StringBuilder procCode = new StringBuilder();
	private int compNum = 0;
	private int whileNum = 0;
	private int ifNum = 0;
	public void run(final String inputFileName, final String outputFileName) {
		String caslCode = null;
		try {
			tokens.buildTokens(inputFileName);
			writeCode("CASL	START	BEGIN\n");
			writeCode("BEGIN	LAD	GR6, 0\n");
			writeCode("	LAD	GR7, LIBBUF\n");
			program();
			tokens.isEnd();
			writeCode("	RET\n");
			System.out.println("OK");
			String subCode = procCode.toString();
			code.append(subCode);
			writeCode("VAR	DS	" + symbolTable.getVarNum() + "\n");
			charTable.stream().forEach(ch -> writeCode(ch));
			writeCode("LIBBUF	DS	256\n");
			writeCode("	END\n");
			caslCode = code.toString();
		}catch (IOException e) { // 例外をつかむ．
			System.err.println("File not found"); // err出力
		}catch (EnshudSyntaxError e) { // 例外をつかむ．
			System.err.println("Syntax error: line " + e.getLineNumber()); // 行番号と共にerr出力
		}catch (EnshudSemanticError e) { // 例外をつかむ．
			System.err.println("Semantic error: line " + e.getLineNumber()); // 行番号と共にerr出力
		}

		try {
            FileWriter file = new FileWriter(outputFileName);
            PrintWriter pw = new PrintWriter(new BufferedWriter(file));

            pw.println(caslCode);

            pw.close();
    		CaslSimulator.appendLibcas(outputFileName);
		}catch (IOException e) {
            e.printStackTrace();
        }

	}

	public class Tokens{
		private int counter;
		private int tokenNum;
		private ArrayList<Token> tokenSet = new ArrayList<>();

		public void buildTokens(final String inputFileName) throws IOException {
			this.counter = -1;
			Path path = Paths.get(inputFileName);
			Charset charset = StandardCharsets.UTF_8;
			BufferedReader reader = Files.newBufferedReader(path, charset);
			String line;
			while ((line = reader.readLine()) != null) {
				Token element = new Token(line);
				tokenSet.add(element);
			}
			tokenNum = tokenSet.size();
		}

		public String next() {
			return tokenSet.get(++counter).getToken();
		}
		public String getNextID() {
			return tokenSet.get(counter+1).getID();
		}
		public String getNextToken() {
			return tokenSet.get(counter+1).getToken();
		}
		public String getCurrentLineNumber() {
			return tokenSet.get(counter).getLineNum();
		}
		public String getCurrentToken() {
			return tokenSet.get(counter).getToken();
		}
		public void inc() {
			this.counter++;
		}
		public void isEnd() throws EnshudSyntaxError{
			if(this.counter < this.tokenNum -1 ) {
				this.counter++;
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
		}
	}

	public class Token{
		String tokenInfo[] = new String[4];

		Token(String line){
			int end = line.lastIndexOf("'");
			if(end == -1) {
				this.tokenInfo = line.split("\t");
			}else {
				String tmp[] = new String[3];
				this.tokenInfo[0] = line.substring(0, end+1);
				tmp = line.substring(end+2).split("\t");
				for(int i = 1; i < 4; i++) {
					this.tokenInfo[i] = tmp[i-1];
				}
			}
		}

		public String getToken() {
			return tokenInfo[0];
		}
		public String getID() {
			return tokenInfo[2];
		}
		public String getLineNum() {
			return tokenInfo[3];
		}
	}

	class EnshudSyntaxError extends Exception{
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		String lineNumber; // 何行目で構文エラーが起きたかを保持する．

		EnshudSyntaxError(String lineNumber) {
			this.lineNumber = lineNumber; // コンストラクタで行番号を保持．
		}

		private String getLineNumber() {
			return lineNumber;
		}
	}

	class EnshudSemanticError extends Exception{
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		String lineNumber; // 何行目で意味エラーが起きたかを保持する．

		EnshudSemanticError(String lineNumber) {
			this.lineNumber = lineNumber; // コンストラクタで行番号を保持．
		}

		private String getLineNumber() {
			return lineNumber;
		}
	}

	public class SymbolTable{
		private ArrayList<Symbol> symbolSet = new ArrayList<>();
		private int start = 0, end = 0;
		private boolean isArr = false;
		private int varNum = 0, funcNum = 0, length, min;

		public void addVar(String name) throws EnshudSemanticError {
			if(isSymbol(name)) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
			symbolSet.add(new Symbol(name));
			this.end++;
		}
		public int addFunc(String name) throws EnshudSemanticError {
			if(isSymbol(name)) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
			Symbol subFunc = new Symbol(name);
			subFunc.setFunc();
			symbolSet.add(subFunc);
			subFunc.setNum(this.funcNum);
			this.end++;
			return this.funcNum++;
		}
		public void setArr(int min, int max) {
			this.isArr = true;
			this.min = min;
			this.length = max - min + 1;
		}
		public void setArg(String name, int length) {
			for(int i = 0; i < this.end; i++) {
				if(symbolSet.get(i).getName().equals(name))
					symbolSet.get(i).setLength(length);
			}
		}
		public void addType(String type) {
			Symbol symbol;
			while(this.start != this.end) {
				symbol = symbolSet.get(this.start);
				symbol.setType(type);
				symbol.setNum(this.varNum);
				if(this.isArr) {
					symbol.setArr();
					symbol.setMin(this.min);
					symbol.setLength(this.length);
					this.varNum += this.length;
				}else {
					this.varNum++;
				}
				this.start++;
			}
			this.isArr = false;
		}
		public boolean isSymbol(String name) {
			for(int i = 0; i < this.end; i++) {
				if(symbolSet.get(i).getName().equals(name))
					return true;
			}
			return false;
		}
		public String symbolType(String name) {
			for(int i = 0; i < this.end; i++) {
				if(symbolSet.get(i).getName().equals(name))
					return symbolSet.get(i).getType();
			}
			return null;
		}
		public boolean isArr(String name) {
			for(int i = 0; i < this.end; i++) {
				if(symbolSet.get(i).getName().equals(name) && symbolSet.get(i).isArr()) {
					return true;
				}
			}
			return false;
		}
		public boolean isFunc(String name) {
			for(int i = 0; i < this.end; i++) {
				if(symbolSet.get(i).getName().equals(name) && symbolSet.get(i).isFunc()) {
					return true;
				}
			}
			return false;
		}
		public int getVarNum(){
			return this.varNum;

		}
		public int getIndex(String name) {
			for(int i = 0; i < this.end; i++) {
				if(symbolSet.get(i).getName().equals(name))
					return symbolSet.get(i).getNum();
			}
			return -1;
		}
		public int getVarMin(String name) {
			for(int i = 0; i < this.end; i++) {
				if(symbolSet.get(i).getName().equals(name))
					return symbolSet.get(i).getMin();
			}
			return -1;
		}
		public int getLength(String name) {
			for(int i = 0; i < this.end; i++) {
				if(symbolSet.get(i).getName().equals(name))
					return symbolSet.get(i).getLength();
			}
			return -1;
		}
	}

	private class Symbol{
		private String name;
		protected String type;
		private boolean isArr = false;
		private boolean isFunc = false;
		private int min, num;
		private int length = 1;

		Symbol(String name){
			this.name = name;
		}
		public String getName() {
			return this.name;
		}
		public String getType() {
			return this.type;
		}
		public int getNum() {
			return this.num;
		}
		public int getMin() {
			return this.min;
		}
		public int getLength() {
			return this.length;
		}
		public void setType(String type) {
			this.type = type;
		}
		public void setArr() {
			this.isArr = true;
		}
		public void setFunc() {
			this.isFunc = true;
		}
		public boolean isArr() {
			return this.isArr;
		}
		public boolean isFunc() {
			return this.isFunc;
		}
		public void setMin(int min) {
			this.min = min;
		}
		public void setNum(int num) {
			this.num = num;
		}
		public void setLength(int length) {
			this.length = length;
		}
	}

	void program() throws EnshudSyntaxError, EnshudSemanticError{
		if(! tokens.next().equals("program")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		programName();

		if(! tokens.next().equals(";")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		block();

		complexStatement();

		if(! tokens.next().equals(".")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
	}

	void programName()  throws EnshudSyntaxError{
		identifier();
	}

	void block() throws EnshudSyntaxError, EnshudSemanticError{
		variableDeclaration();

		isSub = true;
		subprograms();
		isSub = false;
	}

	void variableDeclaration() throws EnshudSyntaxError, EnshudSemanticError{
		if(! tokens.getNextID().equals("21")){
			return;
		}
		tokens.inc();

		isNew = true;
		variableDeclarations();
		isNew = false;
	}

	void variableDeclarations() throws EnshudSyntaxError, EnshudSemanticError{
		variableNames();

		if(! tokens.next().equals(":")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		type();

		if(! tokens.next().equals(";")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		if(tokens.getNextID().equals("43")) {
			variableDeclarations();
		}
	}

	void variableNames() throws EnshudSyntaxError, EnshudSemanticError{
		variableName();

		if(isSub) {
			if(subSymbolTable.isSymbol(tokens.getCurrentToken())) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
			subSymbolTable.addVar(tokens.getCurrentToken());
		}else {
			if(symbolTable.isSymbol(tokens.getCurrentToken())) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
			symbolTable.addVar(tokens.getCurrentToken());
		}

		if(tokens.getNextID().equals("41")) {
			tokens.inc();
			variableNames();
		}
	}

	void variableName() throws EnshudSyntaxError{
		identifier();
	}

	void type() throws EnshudSyntaxError, EnshudSemanticError {
		if(tokens.getNextID().equals("1")) {
			array();
		}else {
			standard(false);
		}
	}

	void standard(boolean isArr) throws EnshudSyntaxError{
		String type = tokens.next();
		if(! (type.equals("char")||type.equals("integer")||type.equals("boolean"))) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		if(isSub) {
			if(type.equals("char") && isArr) {
				subSymbolTable.addType("String");
			}else {
				subSymbolTable.addType(tokens.getCurrentToken());
			}
		}
		else {
			if(type.equals("char") && isArr) {
				symbolTable.addType("String");
			}else {
				symbolTable.addType(tokens.getCurrentToken());
			}
		}
	}

	void array() throws EnshudSyntaxError, EnshudSemanticError{
		if(! tokens.next().equals("array")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		if(! tokens.next().equals("[")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		int min = min();

		if(! tokens.next().equals("..")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		int max;
		if((max = max()) < min) {
			throw new EnshudSemanticError(tokens.getCurrentLineNumber());
		}

		if(! tokens.next().equals("]")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		if(! tokens.next().equals("of")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		if(isSub) {
			subSymbolTable.setArr(min, max);
		}
		else {
			symbolTable.setArr(min, max);
		}
		standard(true);
	}

	int max() throws EnshudSyntaxError, EnshudSemanticError{
		return digit();
	}

	int min() throws EnshudSyntaxError, EnshudSemanticError{
		return digit();
	}

	int digit() throws EnshudSyntaxError{
		boolean isMinus = sign();

		int num = unsignedDigit();
		if(isMinus) {
			return -num;
		}
		return num;
	}

	boolean sign() {
		if(tokens.getNextID().equals("31")) {
			tokens.inc();
			return true;
		}else if(tokens.getNextID().equals("30")) {
			tokens.inc();
		}
		return false;
	}

	void subprograms() throws EnshudSyntaxError, EnshudSemanticError {
		while(tokens.getNextID().equals("16")) {
			subSymbolTable = new SymbolTable();

			subprogram();

			writeCode("	ADDL	GR8, =" + subSymbolTable.getVarNum() + "\n");
			writeCode("	RET\n");

			if(! tokens.next().equals(";")) {
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
		}
	}

	void subprogram() throws EnshudSyntaxError, EnshudSemanticError {
		isNew = true;
		String procName = subprogramHead();

		int argNum = subSymbolTable.getVarNum();
		symbolTable.setArg(procName, argNum);

		variableDeclaration();
		isNew = false;

		int varNum = subSymbolTable.getVarNum();
		writeCode("	SUBL	GR8, =" + varNum + "\n");
		writeCode("	LD	GR0, GR8\n");
		for(int i = 0; i < argNum; i++) {
			writeCode("	LD	GR1, GR0\n");
			writeCode("	ADDL	GR1, =" + (varNum + argNum - i) + "\n");
			writeCode("	LD	GR1, 0, GR1\n");
			writeCode("	LD	GR2, GR0\n");
			writeCode("	ADDL	GR2, =" + i + "\n");
			writeCode("	ST	GR1, 0, GR2\n");
		}

		complexStatement();
	}

	String subprogramHead() throws EnshudSyntaxError, EnshudSemanticError {
		if(! tokens.next().equals("procedure")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		procedureName();
		String procName = tokens.getCurrentToken();

		formalParameter();

		if(! tokens.next().equals(";")) {
			System.out.println(tokens.getCurrentToken());
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		return procName;
	}

	void procedureName() throws EnshudSyntaxError, EnshudSemanticError {
		identifier();
		if(isNew) {
			if(symbolTable.isSymbol(tokens.getCurrentToken())) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());

			}else {
				int procNum = symbolTable.addFunc(tokens.getCurrentToken());
				writeCode("PROC" + procNum + "	NOP\n");
			}
		}
	}

	void formalParameter() throws EnshudSyntaxError, EnshudSemanticError {
		if(! tokens.getNextID().equals("33")){
			return;
		}
		tokens.inc();

		formalParameters();

		if(! tokens.next().equals(")")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
	}

	void formalParameters() throws EnshudSyntaxError, EnshudSemanticError {
		formalParameterNames();

		if(! tokens.next().equals(":")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		standard(false);

		while(tokens.getNextID().equals("37")) {
			tokens.inc();
			formalParameters();
		}
	}

	void formalParameterNames() throws EnshudSyntaxError, EnshudSemanticError {
		formalParameterName();

		while(tokens.getNextID().equals("41")) {
			tokens.inc();
			formalParameterName();
		}
	}

	void formalParameterName() throws EnshudSyntaxError, EnshudSemanticError{
		identifier();
		if(subSymbolTable.isSymbol(tokens.getCurrentToken())) {
			throw new EnshudSemanticError(tokens.getCurrentLineNumber());
		}
		subSymbolTable.addVar(tokens.getCurrentToken());
	}

	void complexStatement() throws EnshudSyntaxError, EnshudSemanticError{
		if(! tokens.next().equals("begin")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		statements();

		if(! tokens.next().equals("end")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
	}

	void statements() throws EnshudSyntaxError, EnshudSemanticError{
		statement();

		if(! tokens.next().equals(";")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		while(! tokens.getNextID().equals("8")) {
			statement();

			if(! tokens.next().equals(";")){
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
		}
	}

	void statement() throws EnshudSyntaxError, EnshudSemanticError{
		if(tokens.getNextID().equals("10")) {
			tokens.inc();
			int localIfNum = ifNum++;

			if(! expression().equals("boolean")){
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}

			writeCode("IF" + localIfNum + "	POP	GR1\n");
			writeCode("	CPL	GR1, =#0000\n");
			writeCode("	JNZ	ELSE" + localIfNum + "\n");

			if(! tokens.next().equals("then")){
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}

			complexStatement();

			writeCode("	JUMP	IFEND" + localIfNum + "\n");

			ifSecondHalf(localIfNum);

			writeCode("IFEND" + localIfNum + "	NOP\n");
		}else if(tokens.getNextID().equals("22")) {
			int localWhileNum = whileNum++;
			tokens.inc();

			writeCode("WHILE" + localWhileNum + "	NOP\n");

			if(! expression().equals("boolean")){
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}

			if(! tokens.next().equals("do")){
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
			writeCode("	POP	GR1\n");
			writeCode("	CPL	GR1, =#0000\n");
			writeCode("	JNZ	WEND" + localWhileNum + "\n");

			complexStatement();

			writeCode("	JUMP	WHILE" + localWhileNum + "\n");
			writeCode("WEND" + localWhileNum + "	NOP\n");
		}else {
			simpleStatement();
		}
	}

	void ifSecondHalf(int localIfNum) throws EnshudSyntaxError, EnshudSemanticError {
		writeCode("ELSE" + localIfNum + "	NOP\n");
		if(! tokens.getNextID().equals("7")){
			return;
		}
		tokens.inc();

		complexStatement();
	}

	void simpleStatement() throws EnshudSyntaxError, EnshudSemanticError{
		if(tokens.getNextID().equals("18") ||tokens.getNextID().equals("23") ) {
			inOrOutput();
		}else if(tokens.getNextID().equals("2")) {
			complexStatement();
		}else if(symbolTable.isFunc(tokens.getNextToken())) {
			procedureStatement();
		}else {
			assignmentStatement();
		}
	}

	String variable(boolean isAssign) throws EnshudSyntaxError, EnshudSemanticError{
		String type;

		variableName();

		if(isSub && subSymbolTable.isSymbol(tokens.getCurrentToken())) {
			type = subSymbolTable.symbolType(tokens.getCurrentToken());

			writeCode("	LD	GR3" + ", GR0\n");
			int varNum = subSymbolTable.getIndex(tokens.getCurrentToken());
			if(varNum != 0) {
				writeCode("	ADDA	GR3" +", =" + varNum + "\n");
			}

			if(subSymbolTable.isArr(tokens.getCurrentToken())) {
				int min = subSymbolTable.getVarMin(tokens.getCurrentToken());
				int length = symbolTable.getLength(tokens.getCurrentToken());
				if(variableSecondHalf(min)) {
					if(type.equals("String")) {
						type = "char";
					}
					if(!isAssign) {
						writeCode("	LD	GR3" + ", 0, GR3" +"\n");
					}
				}else {
					if(type.equals("String") && !isAssign) {
						writeCode("	PUSH	" + length + "\n");
					}else {
						throw new EnshudSemanticError(tokens.getCurrentLineNumber());
					}
				}
			}else if(!isAssign) {
				writeCode("	LD	GR3" + ", 0, GR3" + "\n");
			}

			writeCode("	PUSH	0, GR3" + "\n");
		}else {
			if(! symbolTable.isSymbol(tokens.getCurrentToken())) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
			type = symbolTable.symbolType(tokens.getCurrentToken());

			writeCode("	LAD	GR3" + ", VAR\n");
			int index = symbolTable.getIndex(tokens.getCurrentToken());
			if(index != 0) {
				writeCode("	ADDA	GR3" +", =" + index + "\n");
			}

			if(symbolTable.isArr(tokens.getCurrentToken())) {
				int min = symbolTable.getVarMin(tokens.getCurrentToken());
				int length = symbolTable.getLength(tokens.getCurrentToken());
				if(variableSecondHalf(min)) {
					if(type.equals("String")) {
						type = "char";
					}
					if(!isAssign) {
						writeCode("	LD	GR3" + ", 0, GR3" + "\n");
					}
				}else {
					if(type.equals("String")) {
						writeCode("	PUSH	" + length + "\n");
					}else {
						throw new EnshudSemanticError(tokens.getCurrentLineNumber());
					}
				}
			}else if(!isAssign) {
				writeCode("	LD	GR3" + ", 0, GR3" + "\n");
			}

			writeCode("	PUSH	0, GR3" + "\n");
		}
		return type;
	}

	boolean variableSecondHalf(int min) throws EnshudSyntaxError, EnshudSemanticError {
		if(!tokens.getNextID().equals("35")) {
			return false;
		}
		tokens.inc();
		writeCode("	PUSH	0, GR3\n");

		if(! expression().equals("integer")){
			throw new EnshudSemanticError(tokens.getCurrentLineNumber());
		}

		writeCode("	POP	GR3\n");

		if(! tokens.next().equals("]")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		writeCode("	POP	GR1\n");
		if(min != 0) {
			writeCode("	SUBA	GR1, =" + min + "\n");
		}
		writeCode("	ADDA	GR3" + ", GR1\n");
		return true;
	}

	void assignmentStatement() throws EnshudSyntaxError, EnshudSemanticError{
		String type1 = variable(true);

		if(symbolTable.isArr(tokens.getCurrentToken())) {
			throw new EnshudSemanticError(tokens.getCurrentLineNumber());
		}
		else if(isSub && subSymbolTable.isArr(tokens.getCurrentToken())) {
			throw new EnshudSemanticError(tokens.getCurrentLineNumber());
		}

		if(! tokens.next().equals(":=")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		if(! type1.equals(expression())) {
			throw new EnshudSemanticError(tokens.getCurrentLineNumber());
		}
		writeCode("	POP	GR1\n");
		writeCode("	POP	GR2\n");
		writeCode("	ST	GR1, 0, GR2\n");
	}

	void procedureStatement() throws EnshudSyntaxError, EnshudSemanticError{
		int procNum;
		procedureName();
		String procName = tokens.getCurrentToken();

		if(! tokens.getNextID().equals("33")) {
			procNum = symbolTable.getIndex(procName);
			writeCode("	CALL	PROC" + procNum + "\n");
			return;
		}
		tokens.inc();

		expressions(false);

		if(! tokens.next().equals(")")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		procNum = symbolTable.getIndex(procName);
		int argNum = symbolTable.getLength(procName);

		writeCode("	CALL	PROC" + procNum + "\n");
		writeCode("	ADDL	GR8, =" + argNum + "\n");
	}

	void expressions(boolean isWrite) throws EnshudSyntaxError, EnshudSemanticError{
		String type = expression();

		if(isWrite) {//writeln
			if(type.equals("char")) {
				writeCode("	POP	GR2\n");
				writeCode("	CALL	WRTCH\n");
			}else if(type.equals("String")) {
				writeCode("	POP	GR2\n");
				writeCode("	POP	GR1\n");
				writeCode("	CALL	WRTSTR\n");
			}else if(type.equals("integer")) {
				writeCode("	POP	GR2\n");
				writeCode("	CALL	WRTINT\n");
			}else {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
		}

		while(tokens.getNextID().equals("41")) {
			tokens.inc();

			type = expression();

			if(isWrite) {//writeln
				if(type.equals("char")) {
					writeCode("	POP	GR2\n");
					writeCode("	CALL	WRTCH\n");
				}else if(type.equals("String")) {
					writeCode("	POP	GR2\n");
					writeCode("	POP	GR1\n");
					writeCode("	CALL	WRTSTR\n");
				}else if(type.equals("integer")) {
					writeCode("	POP	GR2\n");
					writeCode("	CALL	WRTINT\n");
				}else {
					throw new EnshudSemanticError(tokens.getCurrentLineNumber());
				}
			}
		}
		if(isWrite) {
			writeCode("	CALL	WRTLN\n");
		}
	}

	String expression() throws EnshudSyntaxError, EnshudSemanticError{
		String type1 = simpleExpression();

		if(relationalOperator()) {
			String operator = tokens.getCurrentToken();
			String type2 = simpleExpression();
			if(type1.equals("char") || type1.equals("String")) {
				if(! (type2.equals("char") || type2.equals("String"))) {
					throw new EnshudSemanticError(tokens.getCurrentLineNumber());
				}
			}
			else if(!type1.equals(type2)) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
			writeCode("	POP	GR2\n");
			writeCode("	POP	GR1\n");
			writeCode("	SUBA	GR1, GR2\n");
			if(operator.equals("=")) {
				writeCode("	JNZ	COMPX" + compNum + "\n");
			}else if(operator.equals("<>")) {
				writeCode("	JZE	COMPX" + compNum + "\n");
			}else if(operator.equals(">=") || operator.equals("<")) {
				writeCode("	JMI	COMPX" + compNum + "\n");
			}else if(operator.equals(">") || operator.equals("<=")) {
				writeCode("	JPL	COMPX" + compNum + "\n");
			}
			if(operator.equals(">") || operator.equals("<")) {
				writeCode("	PUSH	#FFFF\n");
			}else {
				writeCode("	PUSH	#0000\n");
			}
			writeCode("	JUMP	COMPY" + compNum + "\n");
			if(operator.equals(">") || operator.equals("<")) {
				writeCode("COMPX" + compNum +"	PUSH	#0000\n");
			}else {
				writeCode("COMPX" + compNum +"	PUSH	#FFFF\n");
			}
			writeCode("COMPY" + compNum +"	NOP\n");
			compNum++;
			return "boolean";
		}
		return type1;
	}

	boolean relationalOperator() {//関係演算子を統合
		if(tokens.getNextID().equals("24") || tokens.getNextID().equals("25") || tokens.getNextID().equals("26") || tokens.getNextID().equals("27") || tokens.getNextID().equals("28") || tokens.getNextID().equals("29")) {
			tokens.inc();
			return true;
		}
		return false;
	}

	String simpleExpression() throws EnshudSyntaxError, EnshudSemanticError{
		boolean isMinus = sign();
		String type1 = term();
		String op;
		if(isMinus) {
			if(type1.equals("integer")) {
				writeCode("	POP	GR1\n");
				writeCode("	XOR	GR1, =#FFFF\n");
				writeCode("	ADDA	GR1, =1\n");
				writeCode("	PUSH	0, GR1\n");
			}else {
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
		}
		while((op = additionOperator()) != null) {
			String operator = tokens.getCurrentToken();
			String type2 = term();
			if(!(type1.equals(op)  && type1.equals(type2))) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
			writeCode("	POP	GR2\n");
			writeCode("	POP	GR1\n");
			if(operator.equals("+")) {
				writeCode("	ADDA	GR1, GR2\n");
				writeCode("	PUSH	0, GR1\n");
			}else if(operator.equals("-")) {
				writeCode("	SUBA	GR1, GR2\n");
				writeCode("	PUSH	0, GR1\n");
			}else {
				writeCode("	OR	GR1, GR2\n");
				writeCode("	PUSH	0, GR1\n");
			}
		}
		return type1;
	}

	String additionOperator() {//加法演算子を統合
		if(tokens.getNextID().equals("30") || tokens.getNextID().equals("31") ) {
			tokens.inc();
			return "integer";
		}else if(tokens.getNextID().equals("15")) {
			tokens.inc();
			return "boolean";
		}
		return null;
	}

	String term() throws EnshudSyntaxError, EnshudSemanticError{
		String type1 = factor();
		String op;

		while((op = multiplicationOperator()) != null) {
			String operator = tokens.getCurrentToken();
			String type2 = factor();
			if(!(type1.equals(op)  && type1.equals(type2))) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
			writeCode("	POP	GR2\n");
			writeCode("	POP	GR1\n");
			if(operator.equals("*")) {
				writeCode("	CALL	MULT\n");
				writeCode("	PUSH	0, GR2\n");
			}else if(operator.equals("mod")) {
				writeCode("	CALL	DIV\n");
				writeCode("	PUSH	0, GR1\n");
			}else if(operator.equals("AND")) {
				writeCode("	AND	GR1, GR2\n");
				writeCode("	PUSH	0, GR1\n");
			}else {
				writeCode("	CALL	DIV\n");
				writeCode("	PUSH	0, GR2\n");
			}
		}
		return type1;
	}

	String multiplicationOperator() {//乗法演算子を統合
		if(tokens.getNextID().equals("5") || tokens.getNextID().equals("12") || tokens.getNextID().equals("32")) {
			tokens.inc();
			return "integer";
		}else if(tokens.getNextID().equals("0")) {
			tokens.inc();
			return "boolean";
		}
		return null;
	}

	String factor() throws EnshudSyntaxError, EnshudSemanticError{
		if(tokens.getNextID().equals("43")) {
			return variable(false);
		}
		else if(tokens.getNextID().equals("33")) {
			tokens.inc();

			String type = expression();

			if(! tokens.next().equals(")")) {
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
			return type;
		}else if(tokens.getNextID().equals("13")) {
			tokens.inc();
			String type = factor();
			writeCode("	POP	GR1\n");
			writeCode("	XOR	GR1, =#FFFF\n");
			writeCode("	PUSH	0, GR1\n");
			return type;
		}else {
			return constant();
		}
	}

	void inOrOutput() throws EnshudSyntaxError, EnshudSemanticError{
		if(tokens.getNextID().equals("18")) {
			tokens.inc();
			if( !tokens.getNextID().equals("33")) {
				writeCode("	CALL	RDLN\n");
				return;
			}
			tokens.inc();

			variables();

			writeCode("	CALL	RDLN\n");

			if(! tokens.next().equals(")")){
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
		}else if(tokens.next().equals("writeln")){
			if(! tokens.getNextID().equals("33")) {
				return;
			}
			tokens.inc();
			expressions(true);
			if(! tokens.next().equals(")")){
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
		}else {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
	}

	void variables() throws EnshudSyntaxError, EnshudSemanticError{
		String type = variable(true);
		if(type.equals("char")) {
			writeCode("	POP	GR2\n");
			writeCode("	CALL	RDCH\n");
		}
		else if(type.equals("integer")) {
			writeCode("	POP	GR2\n");
			writeCode("	CALL	RDINT\n");
		}
		else if(type.equals("String")) {
			writeCode("	POP	GR2\n");
			writeCode("	POP	GR1\n");
			writeCode("	CALL	RDSTR\n");
		}
		else {
			throw new EnshudSemanticError(tokens.getCurrentLineNumber());
		}
		while(tokens.getNextID().equals("41")) {
			tokens.inc();
			variables();
		}
	}

	String constant() throws EnshudSyntaxError{
		if(tokens.getNextID().equals("9")) { //trueとfalseを統合
			tokens.inc();
			writeCode("	PUSH	#FFFF\n");
			return "boolean";
		}else if(tokens.getNextID().equals("20")) {
			tokens.inc();
			writeCode("	PUSH	#0000\n");
			return "boolean";
		}else if(tokens.getNextID().equals("45")) { //文字列処理を統合
			string();
			int num = charTable.size();
			String token = tokens.getCurrentToken();

			if(token.length() == 3) {//文字
				writeCode("	LD	GR1, =" + token + "\n");
				writeCode("	PUSH	0, GR1\n");
				return "char";
			}else {//文字列
				charTable.add("CHAR" + num + "	DC	" + token + "\n");
				writeCode("	PUSH	" + (token.length() - 2) + "\n");
				writeCode("	PUSH	CHAR" + num + "\n");
				return "String";
			}
		}else {
			writeCode("	PUSH	" + digit() + "\n");
			return "integer";
		}
	}
	int unsignedDigit() throws EnshudSyntaxError {
		if(! tokens.getNextID().equals("44")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		return Integer.parseInt(tokens.next());

	}

	void string() throws EnshudSyntaxError{
		if(! tokens.getNextID().equals("45")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		tokens.inc();
	}

	void identifier() throws EnshudSyntaxError{
		if(! tokens.getNextID().equals("43")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		tokens.inc();
	}

	void writeCode(String casl) {
		if(isSub) {
			procCode.append(casl);
		}else {
			code.append(casl);
		}
	}
}
