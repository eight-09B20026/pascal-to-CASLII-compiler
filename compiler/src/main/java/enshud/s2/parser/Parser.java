package enshud.s2.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Parser {

	/**
	 * サンプルmainメソッド．
	 * 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// normalの確認
		new Parser().run("data/ts/normal02.ts");
 		new Parser().run("data/ts/normal02.ts");

		// synerrの確認
		new Parser().run("data/ts/synerr01.ts");
		new Parser().run("data/ts/synerr02.ts");
	}

	/**
	 * TODO
	 *
	 * 開発対象となるParser実行メソッド．
	 * 以下の仕様を満たすこと．
	 *
	 * 仕様:
	 * 第一引数で指定されたtsファイルを読み込み，構文解析を行う．
	 * 構文が正しい場合は標準出力に"OK"を，正しくない場合は"Syntax error: line"という文字列とともに，
	 * 最初のエラーを見つけた行の番号を標準エラーに出力すること （例: "Syntax error: line 1"）．
	 * 入力ファイル内に複数のエラーが含まれる場合は，最初に見つけたエラーのみを出力すること．
	 * 入力ファイルが見つからない場合は標準エラーに"File not found"と出力して終了すること．
	 *
	 * @param inputFileName 入力tsファイル名
	 */
	Tokens tokens =new Tokens();
	public void run(final String inputFileName) {
		try {
			tokens.buildTokens(inputFileName);
			program();
			tokens.isEnd();
			System.out.println("OK");
		}catch (IOException e) { // 例外をつかむ．
			System.err.println("File not found"); // err出力
		}catch (EnshudSyntaxError e) { // 例外をつかむ．
			System.err.println("Syntax error: line " + e.getLineNumber()); // 行番号と共にerr出力
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
		public String getCurrentLineNumber() {
			return tokenSet.get(counter).getLineNum();
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

	void program() throws EnshudSyntaxError{
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

	void block() throws EnshudSyntaxError{
		variableDeclaration();

		subprograms();
	}

	void variableDeclaration() throws EnshudSyntaxError{
		if(! tokens.getNextID().equals("21")){
			return;
		}
		tokens.inc();

		variableDeclarations();
	}

	void variableDeclarations() throws EnshudSyntaxError{
		variableNames();

		if(! tokens.next().equals(":")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		type();

		if(! tokens.next().equals(";")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		while(tokens.getNextID().equals("43")) {
			variableDeclarations();
		}
	}

	void variableNames() throws EnshudSyntaxError{
		variableName();

		while(tokens.getNextID().equals("41")) {
			tokens.inc();
			variableName();
		}
	}

	void variableName() throws EnshudSyntaxError{
		identifier();
	}

	void type() throws EnshudSyntaxError {
		if(tokens.getNextID().equals("1")) {
			array();
		}else {
			standard();
		}
	}

	void standard() throws EnshudSyntaxError{
		if(! (tokens.getNextID().equals("11")||tokens.getNextID().equals("4")||tokens.getNextID().equals("3"))) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		tokens.inc();
	}

	void array() throws EnshudSyntaxError{
		if(! tokens.next().equals("array")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		if(! tokens.next().equals("[")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		max();

		if(! tokens.next().equals("..")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		min();

		if(! tokens.next().equals("]")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		if(! tokens.next().equals("of")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		standard();
	}

	void max() throws EnshudSyntaxError{
		digit();
	}

	void min() throws EnshudSyntaxError{
		digit();
	}

	void digit() throws EnshudSyntaxError{
		sign();

		unsignedDigit();
	}

	void sign() {
		if(tokens.getNextID().equals("30")||tokens.getNextID().equals("31")) {
			tokens.inc();
		}
	}

	void subprograms() throws EnshudSyntaxError {
		while(tokens.getNextID().equals("16")) {
			subprogram();

			if(! tokens.next().equals(";")) {
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
		}
	}

	void subprogram() throws EnshudSyntaxError {
		subprogramHead();

		variableDeclaration();

		complexStatement();
	}

	void subprogramHead() throws EnshudSyntaxError {
		if(! tokens.next().equals("procedure")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		procedureName();

		formalParameter();

		if(! tokens.next().equals(";")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
	}

	void procedureName() throws EnshudSyntaxError {
		identifier();
	}

	void formalParameter() throws EnshudSyntaxError {
		if(! tokens.getNextID().equals("33")){
			return;
		}
		tokens.inc();

		formalParameters();

		if(! tokens.next().equals(")")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
	}

	void formalParameters() throws EnshudSyntaxError {
		formalParameterNames();

		if(! tokens.next().equals(":")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		standard();

		while(tokens.getNextID().equals("37")) {
			tokens.inc();
			formalParameterNames();

			if(! tokens.next().equals(":")){
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}

			standard();
		}
	}

	void formalParameterNames() throws EnshudSyntaxError {
		formalParameterName();

		while(tokens.getNextID().equals("41")) {
			tokens.inc();
			formalParameterName();
		}
	}

	void formalParameterName() throws EnshudSyntaxError{
		identifier();
	}

	void complexStatement() throws EnshudSyntaxError{
		if(! tokens.next().equals("begin")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		statements();

		if(! tokens.next().equals("end")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
	}

	void statements() throws EnshudSyntaxError{
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

	void statement() throws EnshudSyntaxError{
		if(tokens.getNextID().equals("10")) {
			tokens.inc();

			expression();

			if(! tokens.next().equals("then")){
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}

			complexStatement();

			ifSecondHalf();
		}else if(tokens.getNextID().equals("22")) {
			tokens.inc();

			expression();

			if(! tokens.next().equals("do")){
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}

			complexStatement();
		}else {
			simpleStatement();
		}
	}

	void ifSecondHalf() throws EnshudSyntaxError {
		if(! tokens.getNextID().equals("7")){
			return;
		}
		tokens.inc();

		complexStatement();
	}

	void simpleStatement() throws EnshudSyntaxError{
		if(tokens.getNextID().equals("18") ||tokens.getNextID().equals("23") ) {
			inOrOutput();

		}else if(tokens.getNextID().equals("2")) {

			complexStatement();

		}else {
			variableNameOrProcedureName();

			assignOrProcesure();
		}
	}

	void variableNameOrProcedureName() throws EnshudSyntaxError {
		identifier();
	}

	void assignOrProcesure() throws EnshudSyntaxError {
		if(tokens.getNextID().equals("35") || tokens.getNextID().equals("40")){
			assignmentStatement();
		}else {
			procedureStatement();
		}
	}

	void variable() throws EnshudSyntaxError{
		variableName();

		variableSecondHalf();
	}

	void variableSecondHalf() throws EnshudSyntaxError {
		if(tokens.getNextID().equals("35")) {
			tokens.inc();
			expression();
			if(! tokens.next().equals("]")) {
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
		}
	}

	void assignmentStatement() throws EnshudSyntaxError{
		variableSecondHalf();

		if(! tokens.next().equals(":=")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		expression();
	}

	void procedureStatement() throws EnshudSyntaxError{
		if(! tokens.getNextID().equals("33")) {
			return;
		}
		tokens.inc();

		expressions();

		if(! tokens.next().equals(")")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
	}

	void expressions() throws EnshudSyntaxError{
		expression();

		while(tokens.getNextID().equals("41")) {
			tokens.inc();

			expression();
		}
	}

	void expression() throws EnshudSyntaxError{

		simpleExpression();

		if(relationalOperator()) {

			simpleExpression();

		}
	}

	boolean relationalOperator() {//関係演算子を統合
		if(tokens.getNextID().equals("24") || tokens.getNextID().equals("25") || tokens.getNextID().equals("26") || tokens.getNextID().equals("27") || tokens.getNextID().equals("28") || tokens.getNextID().equals("29")) {
			tokens.inc();
			return true;
		}
		return false;
	}

	void simpleExpression() throws EnshudSyntaxError{
		sign();

		term();
		while(additionOperator()) {
			term();
		}
	}

	boolean additionOperator() {//加法演算子を統合
		if(tokens.getNextID().equals("15") || tokens.getNextID().equals("30") || tokens.getNextID().equals("31") ) {
			tokens.inc();
			return true;
		}
		return false;
	}

	void term() throws EnshudSyntaxError{
		factor();

		while(multiplicationOperator()) {
			factor();
		}
	}

	boolean multiplicationOperator() {//乗法演算子を統合
		if(tokens.getNextID().equals("0") || tokens.getNextID().equals("5") || tokens.getNextID().equals("12") || tokens.getNextID().equals("32")) {
			tokens.inc();
			return true;
		}
		return false;
	}

	void factor() throws EnshudSyntaxError{
		if(tokens.getNextID().equals("43")) {
			variable();
		}
		else if(tokens.getNextID().equals("33")) {
			tokens.inc();

			expression();

			if(! tokens.next().equals(")")) {
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
		}else if(tokens.getNextID().equals("13")) {
			tokens.inc();
			factor();
		}else {
			constant();
		}
	}

	void inOrOutput() throws EnshudSyntaxError{
		if(tokens.getNextID().equals("18")) {
			tokens.inc();
			if( !tokens.getNextID().equals("33")) {
				return;
			}
			tokens.inc();

			variables();

			if(! tokens.next().equals(")")){
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
		}else if(tokens.next().equals("writeln")){
			if(! tokens.getNextID().equals("33")) {
				return;
			}
			tokens.inc();
			expressions();

			if(! tokens.next().equals(")")){
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
		}else {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
	}

	void variables() throws EnshudSyntaxError{
		variable();
		while(tokens.getNextID().equals("41")) {
			tokens.inc();
			variable();
		}
	}

	void constant() throws EnshudSyntaxError{
		if(tokens.getNextID().equals("9") || tokens.getNextID().equals("20")) { //trueとfalseを統合
			tokens.inc();
			return;
		}else if(tokens.getNextID().equals("45")) { //文字列処理を統合
			string();
			return;
		}else {
			unsignedDigit();
		}
	}
	void unsignedDigit() throws EnshudSyntaxError {
		if(! tokens.getNextID().equals("44")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		tokens.inc();
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
}
