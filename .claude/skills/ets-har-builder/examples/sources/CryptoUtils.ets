/**
 * 加密工具类示例
 * 使用了 crypto-js 库的各种功能
 */
import { CryptoJS } from '@ohos/crypto-js';

export class CryptoUtils {
  /**
   * MD5 哈希
   */
  static md5(text: string): string {
    return CryptoJS.MD5(text).toString(CryptoJS.enc.Hex);
  }

  /**
   * SHA1 哈希
   */
  static sha1(text: string): string {
    return CryptoJS.SHA1(text).toString(CryptoJS.enc.Hex);
  }

  /**
   * SHA256 哈希
   */
  static sha256(text: string): string {
    return CryptoJS.SHA256(text).toString(CryptoJS.enc.Hex);
  }

  /**
   * Base64 编码
   */
  static encodeBase64(text: string): string {
    return CryptoJS.enc.Base64.stringify(CryptoJS.enc.Utf8.parse(text));
  }

  /**
   * Base64 解码
   */
  static decodeBase64(base64: string): string {
    return CryptoJS.enc.Base64.parse(base64).toString(CryptoJS.enc.Utf8);
  }

  /**
   * UTF-8 编码
   */
  static encodeUtf8(text: string): string {
    return CryptoJS.enc.Utf8.stringify(CryptoJS.enc.Utf8.parse(text));
  }

  /**
   * UTF-8 解码
   */
  static decodeUtf8(encoded: string): string {
    return CryptoJS.enc.Utf8.parse(encoded).toString(CryptoJS.enc.Utf8);
  }

  /**
   * 十六进制编码
   */
  static encodeHex(text: string): string {
    return CryptoJS.enc.Hex.stringify(CryptoJS.enc.Utf8.parse(text));
  }

  /**
   * 十六进制解码
   */
  static decodeHex(hex: string): string {
    return CryptoJS.enc.Hex.parse(hex).toString(CryptoJS.enc.Utf8);
  }

  /**
   * 生成随机字节数组
   */
  static generateRandomBytes(bytes: number): string {
    return CryptoJS.lib.WordArray.random(bytes).toString(CryptoJS.enc.Hex);
  }

  /**
   * 生成 WordArray
   */
  static createWordArray(): string {
    const words = CryptoJS.lib.WordArray.create([0x00010203, 0x04050607], 4);
    return words.toString();
  }

  /**
   * 创建 WordArray
   */
  static createWordArrayFromData(data: string): string {
    const words = CryptoJS.lib.WordArray.create(CryptoJS.enc.Utf8.parse(data));
    return words.toString();
  }

  /**
   * 使用编码器
   */
  static encodeWithEncoder(text: string, encoder: string): string {
    const utf8Data = CryptoJS.enc.Utf8.parse(text);

    switch (encoder) {
      case 'hex':
        return CryptoJS.enc.Hex.stringify(utf8Data);
      case 'base64':
        return CryptoJS.enc.Base64.stringify(utf8Data);
      case 'latin1':
        return CryptoJS.enc.Latin1.stringify(utf8Data);
      default:
        return text;
    }
  }

  /**
   * 测试静态方法调用
   */
  static testStaticMethodCall(): string {
    const randomData = CryptoJS.lib.WordArray.random(16);
    return randomData.toString(CryptoJS.enc.Hex);
  }

  /**
   * 测试 AES 加密
   */
  static aesEncrypt(text: string, key: string): string {
    const encrypted = CryptoJS.AES.encrypt(text, key);
    return encrypted.toString();
  }

  /**
   * 测试 AES 解密
   */
  static aesDecrypt(encrypted: string, key: string): string {
    const decrypted = CryptoJS.AES.decrypt(encrypted, key);
    return decrypted.toString(CryptoJS.enc.Utf8);
  }

  /**
   * 测试 HMAC-SHA256
   */
  static hmacSha256(text: string, key: string): string {
    const hmac = CryptoJS.HmacSHA256(text, key);
    return hmac.toString(CryptoJS.enc.Hex);
  }
}