import os

from configuration import logger


def check_matplotlib_env():
    if (
        os.environ.get("ENVIRONMENT", default=None) is not None
        and os.environ.get("ENVIRONMENT").upper() == "JENKINS"
    ):
        print("Console System detected => use Agg matplotlib")
        import matplotlib

        matplotlib.use("Agg")
    else:
        import matplotlib
        import matplotlib.pyplot as plt

        #  Qt4Agg or TkAgg
        # matplotlib.use('Qt4Agg')
        plt.ion()


def find_logger_basefilename(logger):
    """Finds the logger base filename(s) currently there is only one
    """
    log_file = None
    parent = logger.__dict__["parent"]
    if parent.__class__.__name__ == "RootLogger":
        # this is where the file name lives
        for h in logger.__dict__["handlers"]:
            if h.__class__.__name__ == "TimedRotatingFileHandler":
                log_file = h.baseFilename
    else:
        log_file = find_logger_basefilename(parent)

    return log_file


class EmailConnector:

    # email_smtp_host = 'smtp.gmail.com'
    # email_smtp_port = 587
    from configuration import logger

    def __init__(self):
        self.email_host = os.environ.get("EMAIL_HOST", default="smtp.gmail.com")
        self.email_port = os.environ.get("EMAIL_PORT", default="587")
        self.email_password = os.environ.get("EMAIL_PASSWORD", default=None)
        self.email = os.environ.get("EMAIL_FROM", default=None)
        if self.email_password is None:

            logger.error('Set the EMAIL_PASSWORD env')
        if self.email is None:
            logger.error('Set the EMAIL_FROM env')

    def send_email(self, recipient, subject, body, html=None, file_append=[], fromEmail=None, fromName=None):
        if self.email_password is None:
            logger.error(
                "Email password  is None .set environment EMAIL_PASSWORD=> not sending email"
            )
            return
        if self.email is None:
            logger.error(
                "Email email  is None.set environment EMAIL_FROM=> not sending email"
            )
            return

        import smtplib
        import mimetypes
        from email.mime.multipart import MIMEMultipart
        from email import encoders
        from email.mime.audio import MIMEAudio
        from email.mime.base import MIMEBase
        from email.mime.image import MIMEImage
        from email.mime.text import MIMEText
        if fromEmail is None:
            fromEmail = self.email
        try:

            msg = MIMEMultipart()
            if fromName is not None:
                msg["From"] = "{} <{}>".format(fromName, fromEmail)
            else:
                msg["From"] = fromEmail
            msg["To"] = recipient
            msg["Subject"] = subject
            body = body
            msg.attach(MIMEText(body, "plain"))
            if html is not None and isinstance(html, str):
                msg.attach(MIMEText(html, "html"))

            # %% Atachemnt
            if file_append is not None and len(file_append) > 0:
                for fileToSend in file_append:
                    if fileToSend is not None and os.path.isfile(fileToSend):
                        logger.debug("adding file " + fileToSend)

                        ctype, encoding = mimetypes.guess_type(fileToSend)
                        if ctype is None or encoding is not None:
                            ctype = "application/octet-stream"

                        maintype, subtype = ctype.split("/", 1)

                        if maintype == "text":
                            fp = open(fileToSend)
                            # Note: we should handle calculating the charset
                            attachment = MIMEText(fp.read(), _subtype=subtype)
                            fp.close()
                        elif maintype == "image":
                            fp = open(fileToSend, "rb")
                            attachment = MIMEImage(fp.read(), _subtype=subtype)
                            fp.close()
                        elif maintype == "audio":
                            fp = open(fileToSend, "rb")
                            attachment = MIMEAudio(fp.read(), _subtype=subtype)
                            fp.close()
                        else:
                            fp = open(fileToSend, "rb")
                            attachment = MIMEBase(maintype, subtype)
                            attachment.set_payload(fp.read())
                            fp.close()
                            encoders.encode_base64(attachment)
                        attachment.add_header(
                            "Content-Disposition", "attachment", filename=fileToSend
                        )
                        msg.attach(attachment)
            result = False
            counter = 3
            while not result and counter > 0:
                try:
                    server = smtplib.SMTP(self.email_host, self.email_port)
                    server.ehlo()
                    server.starttls()
                    server.ehlo()

                    server.login(self.email, self.email_password)
                    text = msg.as_string()
                    problems = server.sendmail(self.email, recipient, text)
                    server.quit()
                    result = True
                except Exception as e:
                    logger.error(
                        "Error: unable to send email retry[%d] :%s" % (counter, str(e))
                    )
                    result = False
                    counter -= 1
                    os.sleep(5)
            if result:
                logger.info("Successfully sent email")
            else:
                logger.error("Error: unable to send email")

        except:
            logger.error("Error: unable to send email")


